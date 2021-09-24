package ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model;

public class ProductRate {
    private Integer productId;
    private Integer rate;

    public Integer incRate() {
        this.rate++;
        return this.rate;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getRate() {
        return rate;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }
}
