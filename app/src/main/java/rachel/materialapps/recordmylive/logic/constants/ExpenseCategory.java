package rachel.materialapps.recordmylive.logic.constants;

public enum ExpenseCategory {
    TRANSPORT("交通"), ACCOMMODATION("住宿"), MATERIALS("物料"),
    FOOD("饮食"), GATHERING("团建"), SIGHTSEEING("游玩参观"), OTHER("其他");

    private final String displayName;
    ExpenseCategory(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }

    public static ExpenseCategory fromKey(String key) {
        for (ExpenseCategory category : values()) {
            if (category.name().equals(key)) return category;
        }
        return OTHER;
    }
}
