package com.mycompany.projetosd2;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author Anubis
 * <site> <numseq> <timestamp send> <timestamp receive> <hops>
7 1 10965460869347846 2439202704391108 14
* 
* Chen - algoritmo que estima a chegada do próximo heartbeat
* 
 */
public class ProjetoSD2 {

    public static void main(String args[]) throws FileNotFoundException, IOException {
        FileInputStream inputStream;
        Scanner sc;
        int sizeList, p; // sizelist -> tamano da janela / p-> processo monitor
        long margin; //margem de segurança
        String trace; // nome do arquivo de traços
        String[] sArray; // para ler os dados da linha
        String line; //linha inteira

        inputStream = new FileInputStream("C:\\IFSUL\\7_semestre\\SD2\\projetoSD2\\src\\main\\java\\com\\mycompany\\projetosd2\\execplan.txt");
        sc = new Scanner(inputStream, "UTF-8");

        line = sc.nextLine();
        while (sc.hasNextLine()) {
            line = sc.nextLine();
            sArray = line.split(";");
            //<server>;<window size>;<margin>
            if (!sArray[0].equals("#")) {
                p = Integer.parseInt(sArray[0]); // processo monitor
                sizeList = Integer.parseInt(sArray[1]);    // size window   número de heartbeats que vai considerar para o cálculo
                margin = Long.parseLong(sArray[2]); // safety margin      margem de segurança mais x tempo ficará esperando a mensagem
                trace = "C:\\IFSUL\\7_semestre\\SD2\\projetoSD2\\src\\main\\java\\com\\mycompany\\projetosd2\\trace2.log";                
                System.out.println(p + "|" + sizeList + "|" + margin + "|" +
                        trace);
                DFTeste test = new DFTeste(sizeList, margin, trace);
                test.execute();
            }
        }
    }
}
