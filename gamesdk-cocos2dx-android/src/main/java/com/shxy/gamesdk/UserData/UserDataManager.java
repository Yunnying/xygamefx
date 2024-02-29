package com.shxy.gamesdk.UserData;

import android.util.Base64;

import com.google.protobuf.InvalidProtocolBufferException;
import com.shxy.gamesdk.BaseSdk.BaseSdk;

import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import userData.UserData;

/**
 * @author: 翟宇翔
 * @date: 2024/2/28
 */
public class UserDataManager {
    private static  UserData.User user;
    private static final String TAG = "UserDataManager";

    protected static void init(){
        user = userData.UserData.User.newBuilder().build();
    }

    /**
     * 获取个人数据字符串。该字符串为一个protobuf格式的字符串，并且经过压缩和base64转码。
     * @return
     */
    protected static String getProtobufString(){
        return BaseSdk.getStringForKey("userData","");
    }

    /**
     * 保存个人数据字符串。该字符串为一个protobuf格式的字符串，并且经过压缩和base64转码。
     * @param data
     */
    protected static void setProtobufString(String data){
        BaseSdk.setStringForKey("userData",data);
    }

    /**
     * 向protobuf中保存个人信息
     * @param key 一个字符串，表示字段的名称
     * @param value 一个Object对象，表示字段的值
     */
    protected static void setStringForKey(String key, String value){
        //先保存原有的protobuf数据
        UserData.User.Builder userBuilder = user.toBuilder();
        //更新某个protobuf数据
        userBuilder.putStringData(key,value);
        //将更新后的protobuf数据保存到原有的对象中
        user = userBuilder.build();
    }

    protected static String getStringForKey(String key){
       return  user.getStringDataOrDefault(key,"");
    }

    protected static void setIntForKey(String key, int value){
        //先保存原有的protobuf数据
        UserData.User.Builder userBuilder = user.toBuilder();
        //更新某个protobuf数据
        userBuilder.putIntData(key,value);
        //将更新后的protobuf数据保存到原有的对象中
        user = userBuilder.build();
    }

    protected static int getIntForKey(String key){
        return  user.getIntDataOrDefault(key,0);
    }


    protected static void setLongForKey(String key, long value){
        //先保存原有的protobuf数据
        UserData.User.Builder userBuilder = user.toBuilder();
        //更新某个protobuf数据
        userBuilder.putLongData(key,value);
        //将更新后的protobuf数据保存到原有的对象中
        user = userBuilder.build();
    }

    protected static long getLongForKey(String key){
        return  user.getLongDataOrDefault(key, 0L);
    }

    protected static void setDoubleForKey(String key, double value){
        //先保存原有的protobuf数据
        UserData.User.Builder userBuilder = user.toBuilder();
        //更新某个protobuf数据
        userBuilder.putDoubleData(key,value);
        //将更新后的protobuf数据保存到原有的对象中
        user = userBuilder.build();
    }

    protected static double getDoubleForKey(String key){
        return  user.getDoubleDataOrDefault(key,0.f);
    }

    protected static void setBoolForKey(String key, boolean value){
        //先保存原有的protobuf数据
        UserData.User.Builder userBuilder = user.toBuilder();
        //更新某个protobuf数据
        userBuilder.putBoolData(key,value);
        //将更新后的protobuf数据保存到原有的对象中
        user = userBuilder.build();
    }

    protected static boolean getBoolForKey(String key){
        return user.getBoolDataOrDefault(key,false);
    }


    /**
     * 使用deflate算法，对protobuf字符串进行压缩。返回一个Base64编码的字符串。
     * @param data
     * @return 一个经过deflate算法压缩的Base64编码的字符串
     */
    private static String compress(byte[] data){


        // 创建压缩器并设置输入
        Deflater compressor = new Deflater();
        compressor.setInput(data);
        compressor.finish();

        // 创建输出字节缓冲区并压缩数据
        byte[] outputBytes = new byte[16384];
        int compressedDataLength = compressor.deflate(outputBytes);

        // 获取压缩后的数据并将其编码为 Base64
        byte[] compressedBytes = new byte[compressedDataLength];
        System.arraycopy(outputBytes, 0, compressedBytes, 0, compressedDataLength);
        return Base64.encodeToString(compressedBytes, Base64.NO_WRAP);
    }

    /**
     * 对Base64字符串进行解码，然后使用deflate算法进行解压缩，返回一个protobuf字符串。
     * @param base64Data
     * @return 一个原始的protobuf字符串
     */
    private static byte[] decompress(String base64Data){
        // 解码 Base64 数据
        byte[] inputBytes = Base64.decode(base64Data,Base64.NO_WRAP);

        // 创建解压缩器并设置输入
        Inflater decompressor = new Inflater();
        decompressor.setInput(inputBytes);

        // 创建输出字节缓冲区并解压缩数据
        byte[] outputBytes = new byte[16384];
        int resultLength;
        try {
            resultLength = decompressor.inflate(outputBytes);
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Invalid base64 data", e);
        }

        // 获取解压缩后的数据并将其转换为字符串
        byte[] decompressedBytes = new byte[resultLength];
        System.arraycopy(outputBytes, 0, decompressedBytes, 0, resultLength);
        return decompressedBytes;
    }

    /**
     * 读取本地protobuf字符串。该字符串是一个经过deflate算法压缩和Base64编码的protobuf字符串。
     * @return 一个原始的protobuf字符串
     */
    private static byte[] readUserData() throws InvalidProtocolBufferException {
        //使用BaseSdk读取本地protobuf字符串
        String userData = BaseSdk.getStringForKey("userData", "");
        // 使用deflate算法进行解压缩
        byte[] decompressedUserData = decompress(userData);
        // 反序列化这个字符串到protobuf对象
        user = UserData.User.parseFrom(decompressedUserData);
        return decompressedUserData;

    }
    /**
     * 将一个自定义的Protobuf对象转为经过deflate算法压缩和Base64编码后保存到本地。
     * @param user 一个DynamicMessage对象
     */
    private static void saveUserData(UserData.User user){
        //获取原始的protobuf字符串
        byte[] protobufData =  user.toByteArray();
        //使用deflate算法进行压缩
        String userData = compress(protobufData);
        //使用BaseSdk保存这个处理后的字符串
        BaseSdk.setStringForKey("userData", userData);
    }
}
