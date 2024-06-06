package com.usms.offloading.dqn.learning;

public enum MyActionSpace {
    lCompOffYes(0),
    lCompOffNo(1),
    edgCompOffYes(2),
    edgeCompOffNo(3),
    //remoteCompOffYes(4),
    remoteCompOffNo(4);
    private final int ord;
    MyActionSpace (int i){
        this.ord=i;
    }
    public int[] getActionCode(){
        if (this.ord == MyActionSpace.lCompOffYes.ord)
            return new int[]{1, 0, 0, 1};
        else if (this.ord == MyActionSpace.lCompOffNo.ord) {
            return new int[]{1, 0, 0, 0};
        } else if (this.ord == MyActionSpace.edgCompOffYes.ord) {
            return new int[]{0, 1, 0, 1};
        } else if (this.ord == MyActionSpace.edgeCompOffNo.ord) {
            return new int[]{0, 1, 0, 0};
//        } else if (this.ord == MyActionSpace.remoteCompOffYes.ord) {
//            return new int[]{0, 0, 1, 1};
       } else {
            return new int[]{0, 0, 1, 0};
        }
    }


}
