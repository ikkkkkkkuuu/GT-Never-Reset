-- The frame contains plain data and the matrix callbacks accept numbers only.
local function identity(_, id) return id end
local function no() return false end
local function ignore() end
Items = {get=identity}
Tags = {getVanillaTag=identity, getFabricTag=identity}
P = {
    getMainItem=function() return frame.mainItem end, getOffhandItem=function() return "minecraft:air" end,
    getActiveHand=function() return "main" end, isUsingItem=function() return frame.using end,
    getPitch=function() return frame.pitch end, getYaw=function() return frame.yaw end,
    getSpeed=function() return frame.speed end, getYSpeed=function() return frame.vertical end,
    getAge=function() return frame.age end, getSwingCount=function() return frame.swings end,
    isOnGround=function() return frame.ground end, isSneaking=function() return frame.sneak end,
    isTouchingWater=function() return frame.water end, isClimbing=function() return frame.climb end,
    isSwimming=no, isCrawling=no, isUsingRiptide=no, isItemCoolingDown=no
}
I = {
    getName=identity, isOf=function(_, item, id) return item == id end,
    isEmpty=function(_, item) return item == "minecraft:air" end,
    getUseAction=function(_, item) return item == frame.mainItem and frame.action or "none" end,
    isIn=function(_, item, tag) return item == frame.mainItem and frame.tags[tag] or false end,
    isBlock=function(_, item) return item == frame.mainItem and frame.block end,
    shouldTranslateItem=function(_, item) return item == frame.mainItem and frame.translate end,
    isCustomTranslate=function(_, item) return translateItem[item] or false end,
    isThrowable=function(_, item) return item == frame.mainItem and frame.throwable end,
    isLantern=function(_, item) return item == frame.mainItem and frame.lantern or false end,
    isChargedCrossbow=no, setChestOpen=ignore, setShulkerOpen=ignore
}
KeyBindManager = {isKeyPressed=function() return frame.inspect end}
Texture = {of=function(_, namespace, name) return name end}
local sprites = {}
frame.screenParticles = sprites
function advanceScreenParticles(dt)
    if #sprites == 0 or dt <= 0 then return end
    local step, drag = dt * 30, 0.9^(dt * 30)
    for i=#sprites,1,-1 do
        local p=sprites[i]
        p.remaining=math.max(0,p.remaining-0.1*step)
        p.x,p.y,p.z=p.x+p.dx*step,p.y+p.dy*step,p.z+p.dz*step
        p.dx,p.dy,p.dz=p.dx*drag,p.dy*drag,p.dz*drag
        if p.gravity then p.dy=p.dy-0.01*step end
        if p.remaining==0 then p.alpha=math.max(0,p.alpha-10*step) end
        if p.tick then p.tick(p) end
        if p.alpha==0 then table.remove(sprites,i) end
    end
end
particleManager = {addParticle=function(_, particles, gravity, x,y,z, dx,dy,dz, rx,ry,rz,
        drx,dry,drz, size, texture, space, hand, life, blend, duration, alpha, tick)
    if hand=="main" and space=="ITEM" and life=="SPAWN" and blend=="ADDITIVE" and duration==0 then
        frame.glow={x=x,y=y,z=z,size=size,texture=texture,alpha=alpha}
    elseif hand=="main" and space=="SCREEN" and life=="OPACITY" and blend=="TRANSLUCENT_L" then
        sprites[#sprites+1]={x=x,y=y,z=z,dx=dx,dy=dy,dz=dz,size=size,texture=texture,
            alpha=alpha,remaining=duration,gravity=gravity,tick=tick}
    end
end}
S = {playSound=ignore}
for _, name in ipairs({"renderAsBlock","applyBlockRotation","translateItem","itemSwingSpeed","usingItem","useDuration"}) do
    local store = {}
    store.put=function(self, key, value) self[key]=value end
    store.getOrDefault=function(self, key, fallback)
        if self[key] == nil then return fallback end
        return self[key]
    end
    maps[name] = store
end
M.clamp=function(_, n, lo, hi) return math.max(lo, math.min(hi, n)) end
M.lerp=function(_, t, a, b) return a + t * (b - a) end
for _, name in ipairs({"abs","sin","cos","pow"}) do
    M[name]=function(_, ...) return math[name](...) end
end
local back = 1.70158
Easings = {
    cubicEase=function(_, t) return t*t*(3-2*t) end,
    easeInSine=function(_, t) return 1-math.cos(math.pi*t/2) end,
    easeInQuart=function(_, t) return t^4 end,
    easeOutQuart=function(_, t) return 1-(1-t)^4 end,
    easeInOutSine=function(_, t) return (1-math.cos(math.pi*t))/2 end,
    easeInBack=function(_, t) return (back+1)*t^3-back*t^2 end,
    easeOutBack=function(_, t) local x=t-1 return 1+(back+1)*x^3+back*x^2 end,
    easeInOutBack=function(_, t)
        local b=back*1.525
        if t < .5 then local x=2*t return x*x*((b+1)*x-b)/2 end
        local x=2*t-2 return (x*x*((b+1)*x+b)+2)/2
    end,
    easeInOutExpo=function(_, t)
        if t == 0 or t == 1 then return t end
        if t < .5 then return 2^(20*t-10)/2 end
        return (2-2^(-20*t+10))/2
    end
}
