-- mx synths lite - nb edition v1.0 @sonoCircuit
-- big thanks to @infinitedigits for mx synths!

local tx = require 'textentry'
local mu = require 'musicutil'
local md = require 'core/mods'
local vx = require 'voice'

local preset_path = "/home/we/dust/data/nb_mxsynths/mxsynth_patches"
local default_patch = "/home/we/dust/data/nb_mxsynths/mxsynth_patches/epiano.patch"
local failsafe_patch = "/home/we/dust/code/nb_mxsynths/data/mxsynth_patches/epiano.patch"
local current_patch = ""

local NUM_VOICES = 6

local synthmodels = {"synthy", "icarus", "casio", "malone", "toshiya", "piano", "epiano", "mdapiano", "kalimba", "triangles", "aaaaaa"}
local synthdef = "mx_synthy"
local is_active = false

local paramlist = {
  "amp", "pan", "send_a", "send_b",
  "model", "sub", "pitchbend", "mod1", "mod2", "mod3", "mod4",
  "modmod1", "modmod2", "modmod3", "modmod4", "send_a_mod", "send_b_mod",
  "attack", "decay", "sustain", "release"
}


--------------------------- osc msgs ---------------------------

local function init_nb_mxsynths()
  osc.send({ "localhost", 57120 }, "/nb_mxsynths/init")
end

local function free_nb_mxsynths()
  osc.send({ "localhost", 57120 }, "/nb_mxsynths/free")
end

local function dont_panic()
  osc.send({ "localhost", 57120 }, "/nb_mxsynths/panic")
end

local function set_param(key, val)
  osc.send({ "localhost", 57120 }, "/nb_mxsynths/set_param", {key, val})
end


--------------------------- utils ---------------------------

local function round_form(param, quant, form)
  return(util.round(param, quant)..form)
end

local function pan_display(param)
  if param < -0.01 then
    return ("L < "..math.abs(util.round(param * 100, 1)))
  elseif param > 0.01 then
    return (math.abs(util.round(param * 100, 1)).." > R")
  else
    return "> <"
  end
end


--------------------------- save and load ---------------------------

local function save_synth_patch(txt)
  if txt then
    local patch = {}
    for _, v in pairs(paramlist) do
      patch[v] = params:get("nb_mxsynths_"..v)
    end
    tab.save(patch, preset_path.."/"..txt..".patch")
    current_patch = txt
    params:set("nb_mxsynths_load_patch", preset_path.."/"..txt..".patch", true)
    print("saved mxsynths patch: "..txt)
  end
end

local function load_synth_patch(path)
  if is_active then
    if path ~= "cancel" and path ~= "" then
      dont_panic()
      if path:match("^.+(%..+)$") == ".patch" then
        local patch = tab.load(path)
        if patch ~= nil then
          for k, v in pairs(patch) do
            params:set("nb_mxsynths_"..k, v)
          end
          local name = path:match("[^/]*$")
          current_patch = name:gsub(".patch", "")
          print("loaded mxsynths patch: "..current_patch)
        else
          if util.file_exists(failsafe_patch) then
            load_synth_patch(failsafe_patch)
          end
          print("error: could not find patch", path)
        end
      else
        print("error: not a mxsynths patch file")
      end
    end
  end
end


--------------------------- params ---------------------------

local modparams = {  
  synthy = {
    name = {"stereo", "lowcut", "resonance", "detune"},
    formatter = {
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linexp(-1, 1, 40, 11000, param:get()), 1, " hz") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end
    },
    hide = {}
  },
  casio = {
    name = {"artifacts", "phasing", "resonance", "detune"},
    formatter = {
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end
    },
    hide = {"sub"}
  },
  icarus = {
    name = {"feedback", "delay time", "pwm width", "detune"},
    formatter = {
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0.05, 0.6, param:get()), 0.01, "s") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end
    },
    hide = {}
  },
  epiano = {
    name = {"mix", "mod index", "lfo speed", "lfo depth"},
    formatter = {
      function(param) return round_form(util.linlin(-1, 1, 0, 0.4, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linexp(-1, 1, 0.01, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 6, param:get()), 0.01, " hz") end,
      function(param) return round_form(util.linexp(-1, 1, 0.01, 1, param:get()) * 100, 1, "%") end
    },
    hide = {"sub"}
  },
  toshiya = {
    name = {"detune", "klanky", "lowcut", "chorus"},
    formatter = {
      function(param) return round_form(util.linexp(-1, 1, 0.01, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0.05, 0.6, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linexp(-1, 1, 25, 11000, param:get()), 1, " hz") end,
      function(param) return round_form(util.linexp(-1, 1, 0.01, 1, param:get()) * 100, 1, "%") end
    },
    hide = {"sub"}
  },
  malone = {
    name = {"detune rate", "detune range", "filter tracking", "resonance"},
    formatter = {
      function(param) return round_form(util.linexp(-1, 1, 0.1, 10, param:get()), 0.01, " hz") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end
    },
    hide = {}
  },
  kalimba = {
    name = {"mix", "spread", "vibrato rate", "vibrato depth"},
    formatter = {
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linexp(-1, 1, 0.01, 16, param:get()), 0.01, " hz") end,
      function(param) return round_form(util.linlin(-1, 1, 0.01, 1, param:get()) * 100, 1, "%") end
    },
    hide = {"sub", "sustain", "release"}
  },
  mdapiano = {
    name = {"detune", "spread", "vibrato rate", "vibrato depth"},
    formatter = {
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0.03, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linexp(-1, 1, 0.01, 16, param:get()), 0.01, " hz") end,
      function(param) return round_form(util.linlin(-1, 1, 0.01, 1, param:get()) * 100, 1, "%") end
    },
    hide = {"sub", "attack", "decay", "sustain"}
  },
  piano = {
    name = {"string decay", "noise freq", "resonance", "detune"},
    formatter = {
      function(param) return round_form(util.linlin(-1, 1, 0.2, 8, param:get()), 0.01, "s") end,
      function(param) return round_form(util.linexp(-1, 1, 400, 16000, param:get()), 1, " hz") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end
    },
    hide = {"sub", "attack", "decay", "sustain"}
  },
  aaaaaa = {
    name = {"voice", "vowel", "detune", "resonance"},
    formatter = {
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end
    },
    hide = {"sub"}
  },
  triangles = {
    name = {"bellow", "decimation", "tune 2nd", "vibrato"},
    formatter = {
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end,
      function(param) return round_form(util.linlin(-1, 1, -24, 24, param:get()), 1, "st") end,
      function(param) return round_form(util.linlin(-1, 1, 0, 1, param:get()) * 100, 1, "%") end
    },
    hide = {"sub"}
  }
}

local function set_modparams(idx)
  local s = modparams[synthmodels[idx]]
  -- set params
  for i = 1, 4 do
    local p = params:lookup_param("nb_mxsynths_mod"..i)
    p.name = s.name[i]
    p.formatter = s.formatter[i]
    p:bang()
    local m = params:lookup_param("nb_mxsynths_modmod"..i)
    m.name = s.name[i]
    m:bang()
  end
  -- hide n seek
  params:show("nb_mxsynths_sub")
  params:show("nb_mxsynths_attack")
  params:show("nb_mxsynths_decay")
  params:show("nb_mxsynths_sustain")
  params:show("nb_mxsynths_release")
  for _, v in ipairs(s.hide) do
    if v then
      params:hide("nb_mxsynths_"..v)
    end
  end
  _menu.rebuild_params()
end

local function add_params()
  params:add_group("nb_mxsynths_group", "mxsynths", 29)
  params:hide("nb_mxsynths_group")

  params:add_separator("nb_mxsynths_patches", "presets")

  params:add_file("nb_mxsynths_load_patch", ">> load", default_patch)
  params:set_action("nb_mxsynths_load_patch", function(path) load_synth_patch(path) end)

  params:add_trigger("nb_mxsynths_save_patch", "<< save")
  params:set_action("nb_mxsynths_save_patch", function() tx.enter(save_synth_patch, current_patch) end)

  params:add_separator("nb_mxsynths_levels", "levels")
  params:add_control("nb_mxsynths_amp", "amp", controlspec.new(0, 1, "lin", 0, 0.8), function(param) return round_form(param:get() * 100, 1, "%") end)
  params:set_action("nb_mxsynths_amp", function(val) set_param('amp', val) end)

  params:add_control("nb_mxsynths_pan", "pan", controlspec.new(-1, 1, "lin", 0, 0), function(param) return pan_display(param:get()) end)
  params:set_action("nb_mxsynths_pan", function(val) set_param('pan', val) end)

  params:add_control("nb_mxsynths_send_a", "send a", controlspec.new(0, 1, "lin", 0, 0), function(param) return round_form(param:get() * 100, 1, "%") end)
  params:set_action("nb_mxsynths_send_a", function(val) set_param('sendA', val) end)
  
  params:add_control("nb_mxsynths_send_b", "send b", controlspec.new(0, 1, "lin", 0, 0), function(param) return round_form(param:get() * 100, 1, "%") end)
  params:set_action("nb_mxsynths_send_b", function(val) set_param('sendB', val) end)

  params:add_separator("nb_mxsynths_sound", "sound")

  params:add_option("nb_mxsynths_model", "model", synthmodels, 1)
  params:set_action("nb_mxsynths_model", function(idx) synthdef = "mx_"..synthmodels[idx] set_modparams(idx) end)

  params:add_control("nb_mxsynths_sub", "sub", controlspec.new(0, 1, "lin", 0, 0.8), function(param) return round_form(param:get() * 100, 1, "%") end)
  params:set_action("nb_mxsynths_sub", function(val) set_param('sub', val) end)

  for i = 1, 4 do
    params:add_control("nb_mxsynths_mod"..i, "mod "..i, controlspec.new(-1, 1, "lin", 0, 0), function(param) return round_form(param:get() * 100, 1, "%") end)
    params:set_action("nb_mxsynths_mod"..i, function(val) set_param('mod'..i, val) end)
  end

  params:add_number("nb_mxsynths_pitchbend", "pitchbend", 1, 24, 7, function(param) return param:get().." st" end)
  params:set_action("nb_mxsynths_pitchbend", function(val) set_param('bndAmt', val) end)

  params:add_separator("nb_mxsynths_env", "envelope")

  params:add_control("nb_mxsynths_attack", "attack", controlspec.new(0.001, 10, "exp", 0, 0.001), function(param) return (round_form(param:get(), 0.01, " s")) end)
  params:set_action("nb_mxsynths_attack", function(val) set_param('attack', val) end)

  params:add_control("nb_mxsynths_decay", "decay", controlspec.new(0.01, 10, "exp", 0, 2.2), function(param) return (round_form(param:get(), 0.01, " s")) end)
  params:set_action("nb_mxsynths_decay", function(val) set_param('decay', val) end)

  params:add_control("nb_mxsynths_sustain", "sustain", controlspec.new(0, 1, "lin", 0, 0.5), function(param) return round_form(param:get() * 100, 1, "%") end)
  params:set_action("nb_mxsynths_sustain", function(val) set_param('sustain', val) end)

  params:add_control("nb_mxsynths_release", "release", controlspec.new(0.01, 10, "exp", 0, 2.2), function(param) return (round_form(param:get(), 0.01, " s")) end)
  params:set_action("nb_mxsynths_release", function(val) set_param('release', val) end)

  params:add_separator("nb_mxsynths_modmods", "modulation")

  params:add_control("nb_mxsynths_mod_amt", "mod amt [map me]", controlspec.new(0, 1, "lin", 0, 0), function(param) return round_form(param:get() * 100, 1, "%") end)
  params:set_action("nb_mxsynths_mod_amt", function(val) set_param('modDepth', val) end)
  params:set_save("nb_mxsynths_mod_amt", false)

  for i = 1, 4 do
    params:add_control("nb_mxsynths_modmod"..i, "modmod "..i, controlspec.new(-1, 1, "lin", 0, 0, "", 1/200), function(param) return round_form(param:get() * 100, 1, "%") end)
    params:set_action("nb_mxsynths_modmod"..i, function(val) set_param('mod'..i..'Mod', val) end)
  end

  params:add_control("nb_mxsynths_send_a_mod", "send a", controlspec.new(-1, 1, "lin", 0, 0, "", 1/200), function(param) return round_form(param:get() * 100, 1, "%") end)
  params:set_action("nb_mxsynths_send_a_mod", function(val) set_param('sendAMod', val) end)
  
  params:add_control("nb_mxsynths_send_b_mod", "send b", controlspec.new(-1, 1, "lin", 0, 0, "", 1/200), function(param) return round_form(param:get() * 100, 1, "%") end)
  params:set_action("nb_mxsynths_send_b_mod", function(val) set_param('sendBMod', val) end)

end

--------------------------- nb player ---------------------------

function add_nb_mxsynths_player()
  local player = {
    alloc = vx.new(NUM_VOICES, 2),
    slot = {},
    clk = nil
  }

  function player:describe()
    return {
      name = "nb_mxsynths",
      supports_bend = true,
      supports_slew = false
    }
  end
  
  function player:active()
    if self.name ~= nil then
      if self.clk ~= nil then
        clock.cancel(self.clk)
      end
      self.clk = clock.run(function()
        clock.sleep(0.2)
        if not is_active then
          is_active = true
          params:lookup_param("nb_mxsynths_load_patch"):bang()
          params:show("nb_mxsynths_group")
          if md.is_loaded("fx") == false then
            params:hide("nb_mxsynths_send_a")
            params:hide("nb_mxsynths_send_b")
            params:hide("nb_mxsynths_send_a_mod")
            params:hide("nb_mxsynths_send_b_mod")
          end
          _menu.rebuild_params()
        end
      end)
    end
  end

  function player:inactive()
    if self.name ~= nil then
      if self.clk ~= nil then
        clock.cancel(self.clk)
      end
      self.clk = clock.run(function()
        clock.sleep(0.2)
        if is_active then
          is_active = false
          dont_panic()
          params:hide("nb_mxsynths_group")
          _menu.rebuild_params()
        end
      end)
    end
  end

  function player:stop_all()
    dont_panic()
  end

  function player:modulate(val)
    params:set("nb_mxsynths_mod_amt", val)
  end

  function player:set_slew(s)
  end

  function player:pitch_bend(note, val)
    set_param('bndDepth', val)
  end

  function player:modulate_note(note, key, value)
  end

  function player:note_on(note, vel)
    local freq = mu.note_num_to_freq(note)
    local slot = self.slot[note]
    if slot == nil then
      slot = self.alloc:get()
    end
    local voice = slot.id - 1 -- sc is zero indexed!
    slot.on_release = function()
      osc.send({ "localhost", 57120 }, "/nb_mxsynths/note_off", {voice})
    end
    self.slot[note] = slot
    osc.send({ "localhost", 57120 }, "/nb_mxsynths/note_on", {synthdef, voice, freq, vel})
  end

  function player:note_off(note)
    local slot = self.slot[note]
    if slot ~= nil then
      self.alloc:release(slot)
    end
    self.slot[note] = nil
  end

  function player:add_params()
    add_params()
  end

  if note_players == nil then
    note_players = {}
  end

  note_players["mxsynths"] = player
end


--------------------------- mod zone ---------------------------

local function post_system()
  if util.file_exists(preset_path) == false then
    util.make_dir(preset_path)
    os.execute('cp '.. '/home/we/dust/code/nb_mxsynths/data/mxsynths_patches/*.patch '.. preset_path)
  end
end

local function pre_init()
  init_nb_mxsynths()
  add_nb_mxsynths_player()
end

md.hook.register("system_post_startup", "nb_mxsynths post startup", post_system)
md.hook.register("script_pre_init", "nb_mxsynths pre init", pre_init)
