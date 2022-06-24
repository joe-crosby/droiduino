package com.droiduino.bluetoothconn;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class MyItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private final PinSwitchAdapter  mAdapter;
    int dragStart = -1;
    int dragEnd = -1;

    public MyItemTouchHelperCallback(PinSwitchAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {return true; }

    @Override
    public boolean isItemViewSwipeEnabled() { return false; }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, ItemTouchHelper.UP |
                ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        if (dragStart == -1){
            dragStart = viewHolder.getAdapterPosition();
        }

        dragEnd = target.getAdapterPosition();

//        Integer holderIndex = viewHolder.getAdapterPosition();
//        Integer targetIndex = target.getAdapterPosition();
//        mAdapter.swap(holderIndex, targetIndex);
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        // We only want the active item to change
        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            if (dragStart >= 0 && dragEnd >= 0 && dragEnd != dragStart) {
                mAdapter.swap(dragStart, dragEnd);
                dragStart = -1;
                dragEnd = -1;
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // What do I do here??

        int i = 0;
    }
}