package com.agile4j.model.builder;

/**
 * @author liurenpeng
 * Created on 2020-10-09
 */
public class Test {
    public static void main(String[] args) {
        String xStr = "10,20,30,40,50,60,70,80,90,100";
        String yStr = "77501.75, 89905.78, 98861, 107057.89, 117435.44, 125762.56, 134305.44, 150377.78, 152937.33, 164059.56";
        String[] xArr = xStr.split(",");
        String[] yArr = yStr.split(", ");
        String result = "";
        for (int i = 0; i < xArr.length; i ++) {
            Double x = Double.valueOf(xArr[i]);
            Double y = Double.valueOf(yArr[i]);
            result = result + "[" + (x + "," + y) + "], \n";
        }
        System.out.println(result);

        /*String log = "";
        String[] strArr = log.split("\n");
        List<Long> vos = new LinkedList<>();
        List<Long> views = new LinkedList<>();
        for (String currLog : strArr) {
            int index = currLog.lastIndexOf(":");
            long cost = Long.valueOf(currLog.substring(index + 1, currLog.length()));
            if (currLog.contains("views")) {
                views.add(cost);
            } else {
                vos.add(cost);
            }
        }
        vos = vos.subList(vos.size() - 10, vos.size());
        views = views.subList(views.size() - 10, views.size());
        System.out.println("vos:" + vos);
        System.out.println("views:" + views);*/
    }
}
