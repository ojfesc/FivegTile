package moe.xzr.fivegtile;

interface IFivegController {
    int getBackendType();
    // 成功返回 "OK"，失败返回具体错误原因
    String compatibilityCheckWithMessage(int subId);
    boolean getFivegEnabled(int subId);
    void setFivegEnabled(int subId, boolean enabled);
}
