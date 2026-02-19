package uk.ac.ed.inf.acpAssignment.dto;

import lombok.Data;

@Data
public class Tuple {
    private String item1;
    private String item2;

    private Integer myInt;

    public Tuple(){

    }

    public Tuple(String item1, String item2){
        this.item1 = item1;
        this.item2 = item2;
    }

    /**
     * make usage in Console.out easier
     * @return
     */
    @Override
    public String toString() {
        return "item1: " + this.item1 + " -- item2: " + this.item2;
    }
}
