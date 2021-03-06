package ch.cern.spark.metrics.defined.equation.functions;

import java.text.ParseException;
import java.util.Map;

import ch.cern.spark.metrics.defined.equation.ValueComputable;
import ch.cern.spark.metrics.value.Value;

public interface FunctionCaller {
	
	public String getFunctionRepresentation();
	
	public Class<? extends Value>[] getArgumentTypes();

	public ValueComputable call(ValueComputable... arguments) throws ParseException;

	public default void register(Map<String, FunctionCaller> functions) {
		functions.put(getFunctionRepresentation(), this);
	}

}
