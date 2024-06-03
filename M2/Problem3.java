package M2;
import java.util.Arrays;

public class Problem3 {
    public static void main(String[] args) {
        // Don't edit anything here
        Integer[] a1 = new Integer[]{-1, -2, -3, -4, -5, -6, -7, -8, -9, -10};
        Integer[] a2 = new Integer[]{-1, 1, -2, 2, 3, -3, -4, 5};
        Double[] a3 = new Double[]{-0.01, -0.0001, -.15};
        String[] a4 = new String[]{"-1", "2", "-3", "4", "-5", "5", "-6", "6", "-7", "7"};
        
        bePositive(a1);
        bePositive(a2);
        bePositive(a3);
        bePositive(a4);
    }

    // <T> turns this into a generic so it can take in any datatype, it'll be passed as an Object so casting is required
    static <T> void bePositive(T[] arr){
        System.out.println("Processing Array:" + Arrays.toString(arr));
        // your code should set the indexes of this array
        Object[] output = new Object[arr.length];
        // hint: use the arr variable; don't directly use the a1-a4 variables
        // TODO convert each value to positive
        // set the result to the proper index of the output array and maintain the original data type
        // hint: don't forget to handle the data types properly, the result datatype should be the same as the original datatype
        //jah89, 06/03/2024
        for (int i = 0; i < arr.length; i++) {
            T value = arr[i];
            if (value instanceof Integer) {
                output[i] = Math.abs((Integer) value);
            } else if (value instanceof Double) {
                output[i] = Math.abs((Double) value);
            } else if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.startsWith("-")) {
                    output[i] = strValue.substring(1);
                } else {
                    output[i] = strValue;
                }
            }
        }
        System.out.println("Making each value positive");
    
        System.out.println("Assigning each value to the output array in the same index as original data type");
        // end edit section

        StringBuilder sb = new StringBuilder();
        for(Object i : output){
            if(sb.length() > 0){
                sb.append(",");
            }
            sb.append(String.format("%s (%s)", i, i.getClass().getSimpleName().substring(0,1)));
        }
        System.out.println("Result: " + sb.toString());
    }
}
