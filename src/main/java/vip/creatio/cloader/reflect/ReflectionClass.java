package vip.creatio.cloader.reflect;

import vip.creatio.cloader.msg.Message;

public enum ReflectionClass {

    PacketPlayOutChat                               (RegisterNms("PacketPlayOutChat")),

    ChatMessageType                                 (RegisterNms("ChatMessageType")),
    IChatBaseComponent                              (RegisterNms("IChatBaseComponent")),
    IChatBaseComponent$ChatSerializer               (RegisterNms("IChatBaseComponent$ChatSerializer")),

    PlayerConnection                                (RegisterNms("PlayerConnection")),
    Packet                                          (RegisterNms("Packet")),

    EntityPlayer                                    (RegisterNms("EntityPlayer")),
    
    CraftServer                                     (RegisterCb("CraftServer")),

    CraftScheduler                                  (RegisterCb("scheduler.CraftScheduler")),
    CraftTask                                       (RegisterCb("scheduler.CraftTask")),
    
    CraftPlayer                                     (RegisterCb("entity.CraftPlayer")),

    ;
    public final Class<?> c;
    ReflectionClass(Class<?> clazz) {
        this.c = clazz;
    }

    private static Class<?> RegisterNms(String name) {
        try {
            return ReflectionUtils.getNmsClass(name);
        } catch (ClassNotFoundException e) {
            Message.internal("&4Registration of NMS class from string &6&l" + name + "&4 failed!");
            throw new RuntimeException(e);
        }
    }

    private static Class<?> RegisterCb(String name) {
        try {
            return ReflectionUtils.getCbClass(name);
        } catch (ClassNotFoundException e) {
            Message.internal("&4Registration of CraftBukkit class from string &6&l" + name + "&4 failed!");
            throw new RuntimeException(e);
        }
    }

}
