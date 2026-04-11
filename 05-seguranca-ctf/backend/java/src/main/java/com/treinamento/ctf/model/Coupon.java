package com.treinamento.ctf.model;

public class Coupon {

    private Integer id;
    private String code;
    private Double discount;
    private Integer maxUses;
    private Integer uses;
    private Integer active;

    public Coupon() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }

    public Integer getUses() { return uses; }
    public void setUses(Integer uses) { this.uses = uses; }

    public Integer getActive() { return active; }
    public void setActive(Integer active) { this.active = active; }
}
