package com.mycompany.projetosd2;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DFTeste extends Thread {

    private int sizeList = 0;
    private final long margin;
    private final String trace;
    private Queue<Long>[] A;
    private final long delta = 100000000; //nanoseconds = 100 miliseconds

    public DFTeste(int sizeList, long margin, String trace) {
        this.sizeList = sizeList;
        this.margin = margin;
        this.trace = trace;
        this.A = new Queue[10];
        for (int j = 0; j < 10; j++) {
            this.A[j] = new LinkedList<>();
        }
    }

    public void execute() throws IOException {
        FileInputStream inputStream;
        Scanner sc;
        String[] stringArray;
        long[] timeout;
        int currentWindowSizeForId, id = 0, lin = 1;
        long ts = 0;
        long[] tPrevious;

        long[] error, timeIniId, timeTotId, mistakeTime;
        long tsIni = 0;
        long totTime = 0;
        error = new long[10];
        timeTotId = new long[10];
        timeIniId = new long[10];
        mistakeTime = new long[10];

        for (int i = 0; i < 10; i++) {
            error[i] = 0;
            timeTotId[i] = 0;
            timeIniId[i] = 0;
            mistakeTime[i] = 0;
        }

        timeout = new long[10];
        tPrevious = new long[10];
        String line = null;

        try {
            inputStream = new FileInputStream(trace);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                line = sc.nextLine();
                stringArray = line.split(" ");
                id = Integer.parseInt(stringArray[0]);
                ts = Long.parseLong(stringArray[3]);
                currentWindowSizeForId = A[id].size();

                if (lin == 1) {
                    tsIni = ts;
                }

                long currentEA = (long) computeEA(currentWindowSizeForId, id);
                timeout[id] = currentEA + margin;

                if ((ts > timeout[id]) && (!A[id].isEmpty())) {
                    mistakeTime[id] += ts - timeout[id];
                    error[id]++;
                }

                if (timeIniId[id] == 0) {
                    timeIniId[id] = ts;
                }
                timeTotId[id] = ts - timeIniId[id];

                if (A[id].size() == this.sizeList) {
                    A[id].poll();
                }
                A[id].add(ts);
                tPrevious[id] = ts;
                lin++;
            }

            if (lin > 1) {
                totTime = ts - tsIni;
            } else {
                totTime = 0;
            }

            double[] taxaErroId = new double[10];
            double[] paId = new double[10];

            String outputFilePath = "C:\\IFSUL\\7_semestre\\SD2\\projetoSD2\\src\\main\\java\\com\\mycompany\\projetosd2\\statistics.log";
            
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
            DecimalFormat df = new DecimalFormat("0.000000000000000", symbols);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
                for (int i = 0; i < 10; i++) {
                    if (i == 1) {
                        continue;
                    }

                    taxaErroId[i] = 0.0;
                    if (totTime > 0) {
                        double totTime_seconds = (double)totTime / 1000000000.0;
                        if (totTime_seconds > 0) {
                            taxaErroId[i] = (double)error[i] / totTime_seconds;
                        } else if (error[i] > 0) { // totTime was > 0 but totTime_seconds became 0 (e.g. totTime < 1ns)
                            taxaErroId[i] = Double.POSITIVE_INFINITY; // Or handle as error[i] / very_small_number
                        }
                    } else if (error[i] > 0 && totTime == 0) { // No total time but errors exist
                        taxaErroId[i] = Double.POSITIVE_INFINITY;
                    }


                    paId[i] = 0.0;
                    if (timeTotId[i] > 0) {
                        paId[i] = (double) mistakeTime[i] / (double) timeTotId[i];
                    }

                    double accuracyProbability = 1.0;
                    if (timeTotId[i] > 0) {
                        if (paId[i] >= 0.0 && paId[i] <= 1.0) {
                            accuracyProbability = 1.0 - paId[i];
                        } else if (paId[i] > 1.0) { // Should ideally not happen if mistakeTime <= timeTotId
                            accuracyProbability = 0.0;
                        }
                        // if paId[i] < 0.0 (also shouldn't happen), it remains 1.0, or handle as error.
                    }
                    
                    String taxaErroStr = df.format(taxaErroId[i]);
                    String accProbStr = df.format(accuracyProbability);

                    String linha = i + ";"
                            + this.sizeList + ";"
                            + margin + ";"
                            + error[i] + ";"
                            + mistakeTime[i] + ";"
                            + timeTotId[i] + ";"
                            + taxaErroStr + ";"
                            + accProbStr;

                    writer.write(linha);
                    writer.newLine();
                }
            } catch (IOException e) {
                Logger.getLogger(DFTeste.class.getName()).log(Level.SEVERE, "Error writing to statistics file", e);
            }

            sc.close();
            inputStream.close();

        } catch (IOException | NumberFormatException ex) {
            Logger.getLogger(DFTeste.class.getName()).log(Level.SEVERE, "Error during trace processing", ex);
        }
    }

    public double computeEA(long currentSamplesInWindow, int id) {
        double totAccumulator = 0, avgEA = 0;
        int k = 0;
        long currentTs;
        try {
            Queue<Long> qCopy = new LinkedList<>(A[id]);
            
            while (!qCopy.isEmpty()) {
                currentTs = qCopy.poll();
                k++;
                totAccumulator += currentTs - (delta * (long)k);
            }

            if (currentSamplesInWindow > 0) {
                avgEA = ((1.0 / (double) currentSamplesInWindow) * totAccumulator) + 
                        (((double) currentSamplesInWindow + 1.0) * delta);
            }
            return avgEA;
        } catch (Exception e) {
            System.err.println("ERRO in computeEA for id " + id + ": " + e.getMessage());
            return 0;
        }
    }
}
