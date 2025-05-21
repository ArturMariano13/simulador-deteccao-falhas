package com.mycompany.projetosd2;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DFTeste extends Thread {

    private int sizeList = 0;
    private final long margin;
    private final String trace;
    // para armazenar os tempos dos heartbeats de cada nodo 
    private Queue<Long>[] A;
    private final long delta = 100000000; //nanoseconds = 100 miliseconds
    private long EA = 0;

    public DFTeste(int sizeList, long margin, String trace) {
        this.sizeList = sizeList;
        this.margin = margin;
        this.trace = trace;
        this.A = new Queue[10];
        for (int j = 0; j < 10; j++) {
            this.A[j] = new LinkedList<>(); //uma lista para cada nodo
        }
    }

    public void execute() throws IOException {
        OutputStream os;
        BufferedWriter bw;
        OutputStreamWriter osw;
        FileInputStream inputStream;
        Scanner sc;
        String[] stringArray; // para ler a linha
        long[] timeout;
        int sizeList, id = 0, lin = 1;
        long ts = 0;
        // ts -> timestamp atual
        long[] tPrevious; // para armazenar o último tempo

        long[] error, timeIniId, timeTotId, mistakeTime;
        long tsIni = 0; // timestamp do primeiro heartbeat
        long totTime = 0; // para o tempo total de execução considerando trace
        error = new long[10]; // número de erros de cada id
        timeTotId = new long[10]; // tempo total de cada id
        timeIniId = new long[10]; // primeiro timestamp de chegada de cada id
        mistakeTime = new long[10];

        for (int i = 0; i < 10; i++) {
            error[i] = 0;
            timeTotId[i] = 0;
            timeIniId[i] = 0;
            mistakeTime[i] = 0;
        }

        String line;
        timeout = new long[10];
        tPrevious = new long[10];

        NumberFormat f = new DecimalFormat("0.000000000000000");
        try {
            inputStream = new FileInputStream(trace);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                line = sc.nextLine();

                stringArray = line.split(" ");
                id = Integer.parseInt(stringArray[0]);
                ts = Long.parseLong(stringArray[3]);
                sizeList = A[id].size();

                if (lin == 1) {
                    tsIni = ts;
                }

                EA = (long) computeEA(sizeList, id);
                timeout[id] = EA + margin;
                //     System.out.println(">" + ts + " - " + timeout[id]);
                if ((ts > timeout[id]) && (!A[id].isEmpty())) {
                    // System.out.println(">" + ts + " - " + timeout[id]);
                    // criar variável para contabilizar os erros
                    /// heartbeat chegou depois da estimativa
                  /// coloca como suspeito
                  mistakeTime[id] += ts - timeout[id];
                    error[id]++;
                } else {
                    
                }

                if (timeIniId[id] == 0) {
                    timeIniId[id] = ts; // tempo inicial do id
                }
                timeTotId[id] = ts - timeIniId[id]; // tempo total do id

                if (A[id].size() == this.sizeList) {
                    A[id].poll(); // retira mais antigo
                }
                A[id].add(ts); // adiciona novo tempo
                tPrevious[id] = ts; // último ts do id
                lin++;
            }// eof - fim de leitura do arquivo

            totTime = ts - tsIni;

            // Gravar novo arquivo
            // para cada identificador calcular taxa de erro, pa
            // tempo de erro do id: quanto tempo ele ficou em erro (ts - timeout[id]) - ir somando a cada vez que entra aqui (para cada identificador soma)
            /*
taxa de erro do id = número de erros do id / tempo total  / 1000000000 
pa do id = tempo de erro do id / tempo total do id 
                    pa = probabilidade de acurácia
            
             
            
            tempo total != tempo total do id
             */
            /// aqui fazer laço para os nodos e gravar dados em arquivo
            
            double[] taxaErroId;
            double[] paId;

            taxaErroId = new double[10];
            paId = new double[10];

            //System.out.println(timeTotId[0]);
            String nomeArquivo = "statistics.log";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\IFSUL\\7_semestre\\SD2\\projetoSD2\\src\\main\\java\\com\\mycompany\\projetosd2\\statistics.log", true))) {  // true para append
                for (int i = 0; i < 10; i++) {
                    if (i == 1) {
                        continue;
                    }
                    taxaErroId[i] = error[i] / totTime / 1000000000;
                    paId[i] = (double) mistakeTime[i] / timeTotId[i];

                    // Formatação para saída com ';' e 15 casas decimais para double
                    String linha = i + ";"
                            + this.sizeList + ";"
                            + margin + ";"
                            + error[i] + ";"
                            + mistakeTime[i] + ";"
                            + timeTotId[i] + ";"
                            + String.format("%.15f", taxaErroId[i]) + ";"
                            + String.format("%.15f", 1 - paId[i]);

                    writer.write(linha);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            /*for (int i = 0; i < 10; i++) {
                taxaErroId[i] = (double) error[i] / (double) totTime / (double) 1000000000;
                paId[i] = (double) mistakeTime[i] / (double) timeTotId[i];

            }*/
            sc.close();
            inputStream.close();

        } catch (IOException | NumberFormatException ex) {
            Logger.getLogger(DFTeste.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public double computeEA(long heartbeat, int id) {
        //id of node
        //heartbeat = highest number of heartbeat sequence received
        double tot = 0, avg = 0;
        int i = 0;
        long ts;
        try {
            NumberFormat f = new DecimalFormat("0.0");
            Queue<Long> q = new LinkedList();
            q.addAll(A[id]);
            while (!q.isEmpty()) {
                ts = q.poll();
                i++;
                tot += ts - (delta * i);
            }
            if (heartbeat > 0) {
                avg = ((1 / (double) heartbeat)
                        * ((double) tot)) + (((double) heartbeat + 1) * delta);
            }
            return avg;
        } catch (Exception e) {
            System.out.println("ERRO " + e.getMessage());
            return 0;
        }
    }

}

// utilizar DOUBLE
// print (id; numero de erros ; margem; taxa de erro; pa)
