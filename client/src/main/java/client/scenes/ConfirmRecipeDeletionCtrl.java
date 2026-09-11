package client.scenes;

import com.google.inject.Inject;

public class ConfirmRecipeDeletionCtrl {

    private PrimaryCtrl pc;

    @Inject
    public ConfirmRecipeDeletionCtrl(PrimaryCtrl p) {
        this.pc = p;
    }

    public void onNo() {
        System.out.println("Deletion cancelled");
        pc.showHome();
    }

    public void onYes() {
        System.out.println("Recipe deleted");
        pc.showHome();
    }
}