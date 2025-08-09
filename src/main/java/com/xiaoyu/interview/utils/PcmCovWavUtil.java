package com.xiaoyu.interview.utils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;

public class PcmCovWavUtil {
    
    /**
     * 将PCM格式的音频流转换为WAV格式
     * @param pcmStream PCM格式的输入流
     * @param wavStream WAV格式的输出流
     */
    public static void convertWaveFile(ByteArrayOutputStream pcmStream, ByteArrayOutputStream wavStream) {
        try {
            // 创建音频格式（16kHz，16bit，单声道，有符号，小端序）
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            byte[] pcmData = pcmStream.toByteArray();
            
            // 创建WAV文件头
            byte[] wavHeader = createWavHeader(pcmData.length, format);
            wavStream.write(wavHeader);
            wavStream.write(pcmData);
        } catch (IOException e) {
            throw new AudioConversionException("PCM转WAV失败", e);
        }
    }

    /**
     * 创建WAV文件头
     * @param pcmDataSize PCM数据大小（字节）
     * @param format 音频格式
     * @return WAV文件头字节数组
     */
    private static byte[] createWavHeader(int pcmDataSize, AudioFormat format) {
        int sampleRate = (int) format.getSampleRate();
        int bitsPerSample = format.getSampleSizeInBits();
        int channels = format.getChannels();
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        int blockAlign = channels * (bitsPerSample / 8);
        
        byte[] header = new byte[44];
        
        // RIFF标识符
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        
        // 文件长度（不包括前8字节）
        int totalSize = 36 + pcmDataSize;
        writeInt(header, 4, totalSize, 4);
        
        // WAVE标识符
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        
        // fmt子块
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        
        // fmt块长度（16字节）
        writeInt(header, 16, 16, 4);
        
        // 音频格式（PCM=1）
        writeInt(header, 20, 1, 2);
        
        // 声道数
        writeInt(header, 22, channels, 2);
        
        // 采样率
        writeInt(header, 24, sampleRate, 4);
        
        // 字节率
        writeInt(header, 28, byteRate, 4);
        
        // 块对齐
        writeInt(header, 32, blockAlign, 2);
        
        // 每样本位数
        writeInt(header, 34, bitsPerSample, 2);
        
        // data标识符
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        
        // data块大小
        writeInt(header, 40, pcmDataSize, 4);
        
        return header;
    }
    
    /**
     * 将整数以小端字节序写入数组
     * @param target 目标数组
     * @param offset 起始位置
     * @param value 要写入的值
     * @param bytes 字节数（1-4）
     */
    private static void writeInt(byte[] target, int offset, int value, int bytes) {
        for (int i = 0; i < bytes; i++) {
            target[offset + i] = (byte) (value >> (i * 8));
        }
    }

    /**
     * 异常类：音频转换异常
     */
    public static class AudioConversionException extends RuntimeException {
        public AudioConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}