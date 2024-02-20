package com.shxy.gamesdk.RemoteData;

import android.util.Base64;
import android.util.Log;

import com.shxy.gamesdk.Login.LoginSdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author: 翟宇翔
 * @date: 2023/12/27
 */
public class RemoteDataManager {
    private static String REMOTE_DATA_URL = "http://example.com";
    private static String REMOTE_APP_ID = "your_app_id";
    private static String mRemoteUserDataStr = "";//用户数据
    private static final String TAG = "RemoteDataManager";
    private static final String defaultStringValue = "unknown";

    /**
     * 初始化
     *
     * @param url   远程数据服务器地址
     * @param appId 远程数据服务器应用ID
     */
    protected static void init(String url, String appId) {
        REMOTE_DATA_URL = url;
        REMOTE_APP_ID = appId;//从本地获取数据
    }

    /**
     * 通过OpenId获取用户数据
     *
     * @param loginType 登录类型
     * @param openId    OpenId
     */
    protected static void getUserDataByOpenId(String loginType, String openId) {
        String url = String.format("%s/api/Players/GetByOpenId?appid=%s&provider=%s&openid=%s", REMOTE_DATA_URL, REMOTE_APP_ID, loginType, openId);
        Log.i(TAG, "getUserDataByOpenId: The url is " + url);
        sendRequest(url, "GET", null, "", new OnResult() {
            @Override
            public void onSuccess(HttpURLConnection connection) {
                onGetUserData(connection);
            }

            @Override
            public void onFailure(int responseCode) {
                Log.e(TAG, "onFailure: getUserDataByOpenId: "+ responseCode);
                RemoteDataSdk.onGetUserData(false, responseCode);
            }
        });
    }

    /**
     * 根据uid获取玩家的存档信息
     *
     * @param uid 玩家的uid
     */
    protected static void getUserDataByUid(String uid) {
        if(uid.isEmpty()){
            Log.e(TAG, "getUserDataByUid: empty uid!");
            return;
        }
        // 构建请求URL
        String url = String.format("%s/api/Players/GetById?appid=%s&uid=%s", REMOTE_DATA_URL, REMOTE_APP_ID, uid);
        // 打印请求URL
        Log.i(TAG, "getUserDataByUid: The url is " + url);
        // 创建一个新线程来处理网络请求，避免在主线程中进行网络操作
        sendRequest(url, "GET", null, "", new OnResult() {
            @Override
            public void onSuccess(HttpURLConnection connection) {
                onGetUserData(connection);
            }

            @Override
            public void onFailure(int responseCode) {
                Log.e(TAG, "onFailure: getUserDataByUid: "+ responseCode);
                RemoteDataSdk.onGetUserData(false, responseCode);
            }
        });
    }

    /**
     * 处理从远程服务器获取的玩家的存档信息
     *
     * @param connection 远程服务器的连接
     */
    private static void onGetUserData(HttpURLConnection connection) {
        // 在这里处理从远程服务器获取的响应数据
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                InputStream in = new BufferedInputStream(connection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                mRemoteUserDataStr = result.toString();
                Log.i(TAG, "onGetUserData: mRemoteUserDataStr is " + mRemoteUserDataStr);
            } else if(responseCode == 404) {
                //返回404，说明不存在这个用户
                Log.i(TAG, "onGetUserData: This user doesn't exist!");
                RemoteDataSdk.onGetUserData(false, responseCode);

            } else {
                Log.e(TAG, "onGetUserData: Error during the connections. ResponseCode is " + responseCode);
                RemoteDataSdk.onGetUserData(false, responseCode);
            }
        } catch (IOException e) {
            Log.e(TAG, "onGetUserData: Error reading stream", e);
            RemoteDataSdk.onGetUserData(false,-1);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 向远程服务器发送玩家的存档信息
     *
     * @param uid  玩家的uid，上传新用户时赋值为空字符串
     * @param data 玩家的存档信息
     * @param properties 玩家的属性信息
     */
    protected static void postUserData(String uid, String data, String properties) {
        String playerId = uid.length() > 0 ? uid : "0";
        String url = String.format("%s/api/Players/SaveV2?appid=%s&uid=%s", REMOTE_DATA_URL, REMOTE_APP_ID, playerId);
        Log.i(TAG, "postUserData: The url is " + url);
        HashMap<String, String> auths = new HashMap<>();
        //String appleId = LoginSdk.getUid(0);  // 使用LoginSdk获取Apple用户的唯一标识
        String appleId = "testappleid1";//测试使用
        if (appleId!= null) {  // 如果appleId不为空
            auths.put("Apple", appleId);  // 将"Apple"和appleId添加到auths中
        }

        String googleId = LoginSdk.getOpenId(2);  // 使用LoginSdk获取Google用户的唯一标识
        if (googleId!= null) {  // 如果googleId不为空
            auths.put("GP", googleId);  // 将"GP"和googleId添加到auths中
        }

        String facebookId = LoginSdk.getOpenId(1);  // 使用LoginSdk获取Facebook用户的唯一标识
        if (facebookId!= null) {  // 如果facebookId不为空
            auths.put("FB", facebookId);  // 将"FB"和facebookId添加到auths中
        }
        Log.i(TAG, "postUserData: The data is " + data);
        // 创建 JSON 数据
        String jsonData = createJson(data, properties, auths);
        // 生成签名
        String signature = generateSignature("66daf9132e309783a6766d47fe462d89", "POST", jsonData, REMOTE_APP_ID, playerId);

        sendRequest(url, "POST", jsonData, signature, new OnResult() {
            @Override
            public void onSuccess(HttpURLConnection connection) {
                onPostUserData(connection);
            }

            @Override
            public void onFailure(int responseCode) {
                Log.e(TAG, "onFailure: postUserData: "+ responseCode);
                RemoteDataSdk.onPostUserData(false, responseCode);
            }
        });
    }

    /**
     * 处理从远程服务器获取的玩家的存档信息
     *
     * @param connection 远程服务器的连接
     */
    private static void onPostUserData(HttpURLConnection connection) {
        try {
            // 获取服务器响应码
            int responseCode = connection.getResponseCode();
            // 处理成功响应码，例如检查是否是 HTTP 200 OK
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 处理成功逻辑，例如从连接中获取响应数据
                // 在成功处理完数据后，可以调用回调方法或者做其他逻辑
                RemoteDataSdk.onPostUserData(true, responseCode);
            } else {
                Log.e(TAG, "onPostUserData: Error during the connections. ResponseCode is " + responseCode);
                // 在处理完错误情况后，调用回调方法或者做其他逻辑
                RemoteDataSdk.onPostUserData(false, responseCode);
            }
        } catch (IOException e) {
            // 处理异常情况
            Log.e(TAG, "onPostUserData: Error reading stream", e);
            RemoteDataSdk.onPostUserData(false, -1); // 传递 -1 表示发生了异常
        } finally {
            // 最后，可以在这里关闭连接等资源
            connection.disconnect();
        }
    }

    /**
     * 删除玩家的存档信息
     *
     * @param uid 玩家的uid
     */
    protected static void deleteUserData(String uid) {
        if(uid.isEmpty()){
            Log.e(TAG, "deleteUserData: empty uid!");
            return;
        }
        String url = String.format("%s/api/Players/DeleteById?appid=%s&uid=%s", REMOTE_DATA_URL, REMOTE_APP_ID, uid);
        Log.i(TAG, "deleteUserData: The url is " + url);
        String signature = generateSignature("66daf9132e309783a6766d47fe462d89", "DELETE","", REMOTE_APP_ID, uid);//删除时body为空
        sendRequest(url, "DELETE", null, signature, new OnResult() {
            @Override
            public void onSuccess(HttpURLConnection connection) {
                onResponseDeleteUserData(connection);
            }

            @Override
            public void onFailure(int responseCode) {
                Log.e(TAG, "onFailure: deleteUserData: "+ responseCode);
                RemoteDataSdk.onGetUserData(false, responseCode);
            }
        });
    }

    /**
     * 处理从远程服务器获取的玩家的存档信息
     *
     * @param connection 远程服务器的连接
     */
    private static void onResponseDeleteUserData(HttpURLConnection connection) {
        try {
            // 获取服务器响应码
            int responseCode = connection.getResponseCode();
            // 处理成功响应码，例如检查是否是 HTTP 204 No Content
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 处理删除成功逻辑，可以根据需要添加其他逻辑
                Log.i(TAG, "onResponseDeleteUserData: Delete Success!");
                RemoteDataSdk.onDeleteUserData(true, responseCode);
            } else {
                // 处理其他响应码，可以根据需要添加更多的逻辑
                // ...

                // 在处理完错误情况后，调用回调方法或者做其他逻辑
                RemoteDataSdk.onDeleteUserData(false, responseCode);
            }
        } catch (IOException e) {
            // 处理异常情况
            e.printStackTrace();
            RemoteDataSdk.onDeleteUserData(false, -1); // 传递 -1 表示发生了异常
        } finally {
            // 最后，可以在这里关闭连接等资源
            connection.disconnect();
        }
    }

    /**
     * 从玩家的存档信息中获取String类型的数据
     *
     * @param key 存档信息的key
     * @return String类型的数据，默认返回""
     */
    protected static String getStringValue(String key) {
        Log.i(TAG, "getStringValue: mRemoteUserDataStr is " + mRemoteUserDataStr);
        if (!mRemoteUserDataStr.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(mRemoteUserDataStr);
                String value = jsonObject.getString(key);
                Log.i(TAG, "getValue: " + value);
                return value;
            }  catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                return "";
            }
        }
        return "";
    }

    /**
     * 从玩家的存档的properties中获取String类型的数据
     *
     * @param key 存档信息的key
     * @return String类型的数据，默认返回""
     */
    protected static String getProperty(String key){
        if (!mRemoteUserDataStr.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(mRemoteUserDataStr);
                String properties = jsonObject.getString("properties");
                JSONObject propertiesJson = new JSONObject(properties);
                String property = propertiesJson.getString(key);
                Log.i(TAG, "getValue: " + property);
                return property;
            }  catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                return "";
            }
        }
        return "";
    }

    /**
     * 从玩家的存档信息中获取openId
     *
     * @param loginType 登录类型，包括"Apple"、"GP"、"FB"
     * @return openId
     */
    protected static String getOpenId(String loginType){
        if (!mRemoteUserDataStr.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(mRemoteUserDataStr);
                String auths = jsonObject.getString("auths");
                JSONObject authsJson = new JSONObject(auths);
                String openId = authsJson.getString(loginType);
                Log.i(TAG, "OpenId: " + openId);
                return openId;
            }  catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                return "";
            }
        }
        return "";
    }

    /**
     * 从玩家的存档信息中获取int类型的数据
     *
     * @param key 存档信息的key
     * @return int类型的数据，默认返回0
     */
    protected static int getIntValue(String key) {
        if (!mRemoteUserDataStr.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(mRemoteUserDataStr);
                int value = jsonObject.getInt(key);
                Log.i(TAG, "getValue: " + value);
                return value;
            }  catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                return 0;
            }
        }
        return 0;
    }

    /**
     * 从玩家的存档信息中获取double类型的数据
     *
     * @param key 存档信息的key
     * @return double类型的数据，默认返回0.0
     */
    protected static double getDoubleValue(String key) {
        if (!mRemoteUserDataStr.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(mRemoteUserDataStr);
                double value = jsonObject.getDouble(key);
                Log.i(TAG, "getValue: " + value);
                return value;
            }  catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * 从玩家的存档信息中获取boolean类型的数据
     *
     * @param key 存档信息的key
     * @return boolean类型的数据，默认返回false
     */
    protected static boolean getBooleanValue(String key) {
        if (!mRemoteUserDataStr.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(mRemoteUserDataStr);
                boolean value = jsonObject.getBoolean(key);
                Log.i(TAG, "getValue: " + value);
                return value;
            }  catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                return false;
            }
        }
        return false;
    }

    /**
     * 网络请求回调接口
     */
    private interface OnResult {
        void onSuccess(HttpURLConnection connection);
        void onFailure(int responseCode);
    }

    /**
     * 发送网络请求
     *
     * @param url      请求的URL
     * @param method   请求的方法
     * @param data     请求的body
     * @param signature 请求的签名
     * @param onResult 网络请求回调接口
     */
    private static void sendRequest(String url, String method, String data, String signature, OnResult onResult) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL requestURL = new URL(url);
                connection = (HttpURLConnection) requestURL.openConnection();
                connection.setRequestMethod(method);

                if (method.equals("POST")) {
                    // 设置请求头
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    connection.setRequestProperty("Sign", signature);
                    Log.i(TAG, "sendRequest: The request head is " + connection.getRequestProperties());
                    // 设置请求体
                    connection.setDoOutput(true);
                    try (OutputStream outputStream = connection.getOutputStream()) {
                        outputStream.write(data.getBytes());
                    }
                } else if (method.equals("DELETE")) {
                    connection.setRequestProperty("Sign", signature);
                }
                // 连接到服务器
                connection.connect();
                // 获取服务器的响应
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    if (method.equals("POST")) {
                        InputStream in = new BufferedInputStream(connection.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                        mRemoteUserDataStr = result.toString();
                    }
                    Log.i(TAG, "sendRequest: Success!");
                    onResult.onSuccess(connection);
                } else{
                    if(method.equals("DELETE") && responseCode == HttpURLConnection.HTTP_NO_CONTENT){
                        Log.i(TAG, "sendRequest: DELETE Success!");
                        onResult.onSuccess(connection);
                    }else{
                        Log.e(TAG, "sendRequest: Fail, the message is " + connection.getResponseMessage());
                        onResult.onFailure(responseCode);
                    }
                }
            } catch (IOException e) {
                onResult.onFailure(-1);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    /**
     * 生成签名
     *
     * @param secretKey 秘钥
     * @param jsonData  待计算的数据
     * @param appid     应用ID
     * @param uid       用户ID
     * @return 签名
     */
    private static String generateSignature(String secretKey, String type, String jsonData, String appid, String uid){
        String url = "";
        String waitSignString = "";
        if(type.equals("POST")){
            url = String.format("/api/Players/SaveV2?appid=%s&uid=%s", appid, uid);
            Log.i(TAG, "generateSignature: The jsonData is: " + jsonData);
            String jsonDataBase64 = encodeJsonToBase64(jsonData);
            Log.i(TAG, "generateSignature: The Base64 of jsonData is：" + jsonDataBase64);
            waitSignString = url + jsonDataBase64;
            Log.i(TAG, "generateSignature: The waitSignString is：" + waitSignString);
        } else if (type.equals("DELETE")) {
            url = String.format("/api/Players/DeleteById?appid=%s&uid=%s", appid, uid);
            waitSignString = url;
            Log.i(TAG, "generateSignature: The waitSignString is：" + waitSignString);
        }else{
            Log.e(TAG, "generateSignature: Wrong type!");
            return "";
        }
        String sign = calculateHmacSha256("66daf9132e309783a6766d47fe462d89", waitSignString);
        Log.i(TAG, "generateSignature: The sign is：" + sign);
        return sign;
    }

    /**
     * 计算HMAC-SHA256
     *
     * @param secretKey 秘钥
     * @param data      待计算的数据
     * @return 计算结果
     */
    private static String calculateHmacSha256(String secretKey, String data) {
        try{
            //byte[] keyBytes = hexStringToByteArray(secretKey);

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            byte[] macData = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // Convert the result from bytes to a hexadecimal string
            StringBuilder result = new StringBuilder();
            for (byte b : macData) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        }catch (NoSuchAlgorithmException e){
            Log.e(TAG, "calculateHmacSha256: No HmacSHA256 Algorithm!");
            return "";
        } catch (InvalidKeyException e) {
            Log.e(TAG, "calculateHmacSha256: InvalidKey!");
            return "";
        }
    }

    /**
     * 用户不存在，创建json字符串
     * @param data 数据
     * @param userProperties 用户属性
     * @param userAuths 用户登录方式
     * @return 创建的json字符串
     */
    private static String createJson(String data, String userProperties, HashMap<String, String> userAuths) {
        JSONObject json = new JSONObject();
        try{
            json.put("id", defaultStringValue);
            json.put("name", defaultStringValue);

            json.put("distinct_Id", defaultStringValue);
            json.put("lastLoginIP", defaultStringValue);
            json.put("uuid", defaultStringValue);
            json.put("appId", REMOTE_APP_ID);
            String time = getCurrentISO8601DateTime();
            json.put("createTime", time);
            json.put("lastUpdateTime",time);
            if(data.isEmpty()){
                Log.i(TAG, "createJson: The data is empty!");
                json.put("data", defaultStringValue);
            }else{
                json.put("data", data);
            }
            json.put("version", 0);
            // 添加 properties 对象
            JSONObject properties = new JSONObject();
            properties.put("userProperties", userProperties);
            json.put("properties", properties);

            // 添加 auths 对象
            JSONObject auths = new JSONObject();
            for (Map.Entry<String, String> entry : userAuths.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                auths.put(key, value);
            }
            json.put("auths", auths);
            return json.toString();
        }catch (JSONException e){
            Log.e(TAG, "Error parsing JSON", e);
            return "";
        }
    }

    /**
     * 获取当前的ISO 8601日期时间字符串
     *
     * @return 当前的ISO 8601日期时间字符串
     */
    private static String getCurrentISO8601DateTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(calendar.getTime());
    }

    /**
     * 将json字符串转换为base64编码
     *
     * @param jsonString json字符串
     * @return base64编码后的字符串
     */
    private static String encodeJsonToBase64(String jsonString) {
        // 将 JSON 字符串转换为字节数组
        byte[] jsonData = jsonString.getBytes();
        // 对字节数组进行 Base64 编码，注意使用NO_WRAP以避免换行符
        byte[] base64Encoded = Base64.encode(jsonData, Base64.NO_WRAP);
        // 将编码后的字节数组转换为字符串
        return new String(base64Encoded);
    }
}