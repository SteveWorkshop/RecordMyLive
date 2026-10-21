package rachel.materialapps.recordmylive.ui.components;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.Consumer;

import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;

public final class ConcertDeleteDialog {
    private ConcertDeleteDialog() {}

    public static AlertDialog show(Context context, ConcertEntity concert,
                                   Consumer<Consumer<Boolean>> deleteAction) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("删除演出")
                .setMessage("确定删除 \"" + concert.getName()
                        + "\" 吗？该演出的歌单和附加费用也会一并删除，且无法恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                    dialog.setCancelable(false);
                    deleteAction.accept(success -> {
                        if (!dialog.isShowing()) return;
                        if (success) {
                            dialog.dismiss();
                            Toast.makeText(context, "演出已删除", Toast.LENGTH_SHORT).show();
                        } else {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                            dialog.setCancelable(true);
                            Toast.makeText(context, "删除失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    });
                }));
        dialog.show();
        return dialog;
    }
}
