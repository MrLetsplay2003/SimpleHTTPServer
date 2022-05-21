package me.mrletsplay.simplehttpserver.http.compression;

public interface HttpCompressionMethod {

	public String getName();
	
	public byte[] compress(byte[] uncompressed);
	
}
