package Crypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by peng on 2016/6/25.
 */
public class Crypt {
    private HashMap<String, Object> keyMap;

    //生成RSA key并保存到“KeyPair”
    void saveKey() {
        try {
            KeyPairGenerator KeyPairGen = KeyPairGenerator.getInstance("RSA");
            final int SIZE=1024;
            KeyPairGen.initialize(SIZE);
            KeyPair Keypair=KeyPairGen.genKeyPair();
            ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream("KeyPair"));
            out.writeObject(Keypair);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    //从文件中加载RSA Key
    public void loadKey(String filename){
        try {
            ObjectInputStream oin = new ObjectInputStream(new FileInputStream(filename));
            KeyPair kp=(KeyPair) oin.readObject();
            keyMap=new HashMap<>();
            keyMap.put("RSA_PUBLIC", kp.getPublic());
            keyMap.put("RSA_PRIVATE", kp.getPrivate());
            keyMap.put("DES_KEY", "12345678");
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    //MD5 hash加RSA签名，签名结果用base64加密以方便传输
    public byte[] sign(byte[] data){
        try {
            Signature sign = Signature.getInstance("MD5withRSA");
            sign.initSign((PrivateKey) keyMap.get("RSA_PRIVATE"));
            sign.update(data);
            return Base64.getEncoder().encode(sign.sign());
        }
        catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        catch (InvalidKeyException e){
            e.printStackTrace();
        }
        catch (SignatureException e){
            e.printStackTrace();
        }

        return null;
    }

    //验证签名，签名需先base64解密
    public boolean verify(byte[] data, byte[] signData){
        try{
            Signature sign=Signature.getInstance("MD5withRSA");
            sign.initVerify((PublicKey)(keyMap.get("RSA_PUBLIC")));
            sign.update(data);
            return sign.verify(Base64.getDecoder().decode(signData));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    //DES加密，加密结果用base再加密以便于传输
    public byte[] encrypt(byte[] data){
        try{
            SecureRandom random = new SecureRandom();
            DESKeySpec desKey = new DESKeySpec(((String)keyMap.get("DES_KEY")).getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, securekey, random);
            return Base64.getEncoder().encode(cipher.doFinal(data));
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //DES解密，解密密结果先用base64解密，再用DES解密
    public byte[] decrypt(byte[] data){
        try{
            SecureRandom random = new SecureRandom();
            DESKeySpec desKey = new DESKeySpec(((String)keyMap.get("DES_KEY")).getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, securekey, random);
            return cipher.doFinal(Base64.getDecoder().decode(data));
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }


    static public void main(String[] args){
        Crypt cp=new Crypt();
        cp.loadKey("KeyPair");
        try {
            ByteBuffer bbuf;
            FileInputStream fin=new FileInputStream("index.html");
            FileChannel fc=fin.getChannel();
            bbuf=ByteBuffer.allocate((int)fc.size());
            while((fc.read(bbuf))>0){

            }
            byte[] buf=bbuf.array();
            byte[] en=cp.encrypt(buf);
            System.out.print(cp.decrypt(en));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
