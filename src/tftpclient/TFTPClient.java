/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tftpclient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 15367519
 */
public class TFTPClient {

    static String DEFAULT_TFTP_SERVER = "127.0.0.1";
    static final int TFTP_DEFAULT_PORT = 69;
    
    static final byte ZERO_BYTE = 0;
    static final String MODE_OCTECT = "octet";
    static final String MODE_ASCII = "netascii";
    static final byte OP_RRQ = 1;
    private static final byte OP_DATA=3;
    private static final byte OP_ACK=4;
    static final byte OP_ERROR = 5;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        TFTPClient client = new TFTPClient();
        client.get("tftpd32.ini");
        try {
            System.out.println(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException ex) {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private InetAddress address;
    private DatagramSocket datagramSocket;
    private byte[] requestByteArray;
    private DatagramPacket outDatagramPacket;
    private byte[] bufferByteArray;
    private DatagramPacket inDatagramPacket;
    

    private void get(String fileName) {
        try {
            address = InetAddress.getByName(DEFAULT_TFTP_SERVER);
            datagramSocket = new DatagramSocket();
            requestByteArray = buildRRQRequest(fileName, MODE_OCTECT);
            outDatagramPacket = new DatagramPacket(requestByteArray,requestByteArray.length,address, TFTP_DEFAULT_PORT);
            datagramSocket.send(outDatagramPacket);
            
            ByteArrayOutputStream byteOutput = receiveFile();
            //System.out.println(byteOutput.toString());
            writeFile(byteOutput,fileName);
        } catch (UnknownHostException ex) {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SocketException ex) {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    public byte[] buildRRQRequest(String fileName,String mode) {
        int packetLen = 2 + fileName.length() + 1 + mode.length() + 1;
        byte[] packet = new byte[packetLen];
        int pos = 0; //posizione all'interno del pacchetto
        packet[pos] = ZERO_BYTE;
        pos++;
        packet[pos] = OP_RRQ;
        pos++;
        for (int i =0;i< fileName.length();i++) {
            packet[pos] = (byte)fileName.charAt(i);
            pos++;
        }
        //FINITO DI CODIFICARE IL NOME DEL FILE
        packet[pos] = ZERO_BYTE;
        pos++;
        for (int i =0;i< mode.length();i++) {
            packet[pos] = (byte)mode.charAt(i);
            pos++;
        }
        packet[pos] = ZERO_BYTE;
        pos++;
        return packet;
    }

    private ByteArrayOutputStream receiveFile() {
        ByteArrayOutputStream byteOuput = new ByteArrayOutputStream();
        int block = 1;
        do {
            try {
                System.out.println("TFTP Packet Count:"+block);
                block++;
                bufferByteArray =new byte[516];
                inDatagramPacket = new DatagramPacket(bufferByteArray,
                        bufferByteArray.length,address,
                        datagramSocket.getLocalPort());
                datagramSocket.receive(inDatagramPacket);
                byte opCode = bufferByteArray[1];
                
                if (opCode == OP_ERROR) {
                    reportError();
                } else if (opCode == OP_DATA) {
                    byte[] blockNumber = {bufferByteArray[2],bufferByteArray[3]};
                    DataOutputStream output = new DataOutputStream(byteOuput);
                    output.write(inDatagramPacket.getData(),4,
                                inDatagramPacket.getLength()-4);
                    
                    sendAck(blockNumber);
                }
                
            } catch (IOException ex) {
                Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
        } while (!isLastPacket(inDatagramPacket));
        return byteOuput;
    }

    private boolean isLastPacket(DatagramPacket packet) {
        if (packet.getLength() < 512) {
            return true;
        } else {
            return false;
        }
    }
    
    private void reportError() {
        String errorCode = new String(bufferByteArray,3,1);
        String errorText = new String(bufferByteArray,4,
                        inDatagramPacket.getLength()-4);
        System.out.println("[" + errorCode + "] " + errorText);        
    }

    private void sendAck(byte[] blockNumber) {
        try {
            byte[] ACK = {0,OP_ACK,blockNumber[0],
                blockNumber[1]};
            DatagramPacket packet = new DatagramPacket(ACK,
                    ACK.length,address,inDatagramPacket.getPort());
            
            datagramSocket.send(packet);
        } catch (IOException ex) {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        }

    private void writeFile(ByteArrayOutputStream byteOutput, String fileName) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(fileName);
            byteOutput.writeTo(out);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(TFTPClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    }
    
    

