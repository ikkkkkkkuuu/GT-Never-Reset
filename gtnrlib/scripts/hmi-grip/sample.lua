-- Neutral right-hand input for the HMI 5.1.1 scripts extracted by convert.py.
local kind = assert(arg[1])
local tag = ({sword="swords", axe="axes", pickaxe="pickaxes", shovel="shovels", hoe="hoes", ingot="ingots"})[kind]
local held, age, record = "minecraft:iron_" .. kind, 0, false
local function no() return false end
local function zero() return 0 end
local function identity(_, value) return value end

P = {getSpeed=zero, getYSpeed=zero, getPitch=zero, getYaw=zero, getSwingCount=zero,
     getAge=function() return age end, getMainItem=function() return held end,
     getOffhandItem=function() return "empty" end, getActiveHand=function() return "main" end,
     isOnGround=function() return true end, isClimbing=no, isCrawling=no, isItemCoolingDown=no,
     isSneaking=no, isSwimming=no, isTouchingWater=no, isUsingItem=no, isUsingRiptide=no}
I = {getUseAction=function() return "none" end, getName=identity, isEmpty=function(_, v) return v=="empty" end,
     isOf=function(_, v, id) return v==id end,
     isIn=function(_, v, t) return v==held and (tag==t or (kind~="ingot" and t=="tools")) end,
     isBlock=no, isCustomTranslate=no, shouldTranslateItem=function() return kind=="ingot" end,
     isThrowable=no, isChargedCrossbow=no, isLantern=no, setChestOpen=no, setShulkerOpen=no}
Tags = {getVanillaTag=identity, getFabricTag=identity}
Items = {get=identity}
KeyBindManager = {isKeyPressed=no}
for _, key in ipairs({"renderAsBlock","translateItem","itemSwingSpeed","applyBlockRotation","usingItem","useDuration"}) do
    _G[key] = {put=function(t,k,v) t[k]=v end,
               getOrDefault=function(t,k,fallback) if t[k]==nil then return fallback end return t[k] end}
end

Easings = luajava.newInstance("com.holdmylua.source.scripting.script_wrappers.Easings")
M = {clamp=function(_,v,lo,hi) return math.max(lo,math.min(v,hi)) end,
     lerp=function(_,t,a,b) return a+(b-a)*t end}
for _, name in ipairs({"sin","cos","abs","pow"}) do
    M[name]=function(_,...) return math[name](...) end
end
for _, name in ipairs({"translate","scale","moveX","moveY","moveZ","rotateX","rotateY","rotateZ","shear"}) do
    M[name]=function(_,matrix,...)
        if record then print(name,...) end
    end
end
context={matrices={}, player="player", item=held, bl=true, mainHand=true, hand="main", deltaTime=1/60,
         equipProgress=0, swingProgress=0, mainHandSwingProgress=0, offHandSwingProgress=0,
         mainHandSwitchEvent=false, offHandSwitchEvent=false, swingMHand=false, swingOHand=false, blockBreaking=false}
local function loadPose(name)
    local file=assert(io.open(name..".lua"))
    local source=file:read("*a")
    file:close()
    source=source:gsub("%${(%w+)}", function(k)
        assert(k=="inspectKeybind" or k=="xOffset" or k=="yOffset" or k=="zOffset",k)
        return "0"
    end)
    source=source:gsub("global%.(%w+)%s*=%s*([^;\n]+);",function(key,expr)
        if _G[key]==nil then _G[key]=assert(load("return "..expr))() end
        return ""
    end)
    return assert(load(source,"@"..name))
end
local shared, arm, item = loadPose("hand_pose"), loadPose("hand_relative_pose"), loadPose("item_pose")
for frame=0,240 do
    age=frame/3
    record=frame==240
    if record then print("BRANCH shared") end
    shared()
    if record then print("BRANCH arm") end
    arm()
    if record then print("BRANCH item") end
    item()
end
print("DONE")
