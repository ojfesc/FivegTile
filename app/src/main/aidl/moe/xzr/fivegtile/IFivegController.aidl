package moe.xzr.fivegtile;

interface IFivegController {
    /**
     * 后端类型：0 = 不可用，1 = Root，2 = Shizuku/Sui。
     * 用于诊断和 UI 展示。
     */
    int getBackendType();

    boolean compatibilityCheck(int subId);
    boolean getFivegEnabled(int subId);
    void setFivegEnabled(int subId, boolean enabled);
}
