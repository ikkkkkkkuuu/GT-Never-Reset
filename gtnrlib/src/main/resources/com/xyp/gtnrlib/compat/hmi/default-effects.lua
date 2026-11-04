local id = frame.mainItem
petal = petal or { ["minecraft:pink_petals"]="textures/blocks/flower_tulip_pink.png",
    ["minecraft:wildflowers"]="textures/blocks/flower_dandelion.png", ["minecraft:leaf_litter"]="textures/blocks/leaves_oak.png" }
local sprite = petal[id]
if sprite and frame.swings ~= lastSwing and context.swingProgress > 0 then
    for i = 1, 5 do
        particleManager:addParticle(nil,true,.22,-.16,-.65,(i-3)*.018,.035+i*.006,-.018,
            0,0,0,0,0,0,.09,sprite,"SCREEN","main","OPACITY","TRANSLUCENT_L",.55,225)
    end
end
if context.swingProgress > 0 or not sprite then lastSwing=frame.swings end
if id == "minecraft:torch" or frame.lantern or id == "minecraft:lava_bucket" or id == "minecraft:soul_torch" then
    frame.glow={x=.5,y=.65,z=.5,size=.65,texture="gtnrlib:textures/animation/glow.png",alpha=120}
end
