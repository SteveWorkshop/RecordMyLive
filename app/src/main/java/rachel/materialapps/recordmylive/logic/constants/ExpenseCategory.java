package rachel.materialapps.recordmylive.logic.constants;

import rachel.materialapps.recordmylive.R;

public enum ExpenseCategory {
    TRANSPORT("交通", R.drawable.baseline_subway_24px),
    ACCOMMODATION("住宿", R.drawable.baseline_hotel_24px),
    MATERIALS("物料", R.drawable.baseline_forklift_24px),
    FOOD("饮食", R.drawable.baseline_dining_24px),
    GATHERING("团建", R.drawable.baseline_diversity_3_24px),
    SIGHTSEEING("游玩参观", R.drawable.baseline_nature_people_24px),
    OTHER("其他", R.drawable.baseline_other_admission_24px);

    private final String displayName;
    private final int iconResource;

    ExpenseCategory(String displayName, int iconResource) {
        this.displayName = displayName;
        this.iconResource = iconResource;
    }
    public String getDisplayName() { return displayName; }
    public int getIconResource() { return iconResource; }

    public static ExpenseCategory fromKey(String key) {
        for (ExpenseCategory category : values()) {
            if (category.name().equals(key)) return category;
        }
        return OTHER;
    }
}
