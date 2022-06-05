package me.mrletsplay.simplehttpserver.dom.css;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CssElement {

	private CssSelector selector;
	private Map<String, Supplier<String>> properties;

	public CssElement(CssSelector selector) {
		this.selector = selector;
		this.properties = new HashMap<>();
	}

	public void setSelector(CssSelector selector) {
		this.selector = selector;
	}

	public CssSelector getSelector() {
		return selector;
	}

	public void setProperty(String name, Supplier<String> value) {
		if(value == null) {
			properties.remove(name);
			return;
		}

		properties.put(name, value);
	}

	public void setProperty(String name, String value) {
		if(value == null) {
			properties.remove(name);
			return;
		}

		setProperty(name, () -> value);
	}

	public Supplier<String> getProperty(String name) {
		return properties.get(name);
	}

	public Map<String, Supplier<String>> getProperties() {
		return properties;
	}

	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(selector.toString()).append("{");
		for(Map.Entry<String, Supplier<String>> prop : properties.entrySet()) {
			b.append(prop.getKey()).append(":").append(prop.getValue().get()).append(";");
		}
		b.append("}");
		return b.toString();
	}

}
