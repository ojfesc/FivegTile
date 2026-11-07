package moe.xzr.fivegtile;

interface IFivegController {
    int getBackendType();
    // 成功返回 "OK"，失败返回具体错误原因
    String compatibilityCheckWithMessage(int subId, int slotId);
    boolean getFivegEnabled(int subId, int slotId);
    void setFivegEnabled(int subId, int slotId, boolean enabled);
}
