package me.mrletsplay.simplehttpserver.dom.js;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class JSScript {
	
	private List<JSFunction> functions;
	private Supplier<String> code;
	
	public JSScript() {
		this.functions = new ArrayList<>();
	}
	
	public void addFunction(JSFunction function) {
		functions.add(function);
	}
	
	public void removeFunction(JSFunction function) {
		functions.remove(function);
	}
	
	public List<JSFunction> getFunctions() {
		return functions;
	}
	
	public void setCode(Supplier<String> code) {
		this.code = code;
	}
	
	public void setCode(String code) {
		setCode(() -> code);
	}
	
	public void appendCode(Supplier<String> code) {
		if(this.code == null) {
			setCode(code);
			return;
		}
		Supplier<String> oldCode = this.code;
		this.code = () -> oldCode.get() + code.get();
	}
	
	public void appendCode(String code) {
		appendCode(() -> code);
	}
	
	public Supplier<String> getCode() {
		return code;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for(JSFunction f : getFunctions()) {
			b.append(f.toString());
		}
		if(getCode() != null) b.append(getCode().get());
		return b.toString();
	}

}
