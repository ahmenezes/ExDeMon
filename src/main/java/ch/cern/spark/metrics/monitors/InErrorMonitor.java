package ch.cern.spark.metrics.monitors;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.spark.streaming.State;

import ch.cern.properties.ConfigurationException;
import ch.cern.properties.Properties;
import ch.cern.spark.metrics.Metric;
import ch.cern.spark.metrics.filter.MetricsFilter;
import ch.cern.spark.metrics.notificator.Notificator;
import ch.cern.spark.metrics.notificator.types.ConstantNotificator;
import ch.cern.spark.metrics.results.AnalysisResult;
import ch.cern.spark.metrics.results.AnalysisResult.Status;
import ch.cern.spark.metrics.store.Store;

public class InErrorMonitor extends Monitor {

	private Exception exception;
	
	private MetricsFilter filter;

	private Map<String, String> tags;
	
	public InErrorMonitor(String id, ConfigurationException e) {
		super(id);
		
		exception = e;
	}

	@Override
	public Monitor config(Properties properties) {
		try {
			filter = MetricsFilter.build(properties.getSubset("filter"));
		} catch (ConfigurationException e) {}
		
		tags = new HashMap<>();
        Properties tagsProps = properties.getSubset("tags");
        Set<String> tagKeys = tagsProps.getUniqueKeyFields();
        tagKeys.forEach(key -> tags.put(key, tagsProps.getProperty(key)));
		
		return this;
	}
	
	@Override
	public Optional<AnalysisResult> process(State<Store> storeState, Metric metric) {
		AnalysisResult result = null;
		
		if(filter != null) {
			result = AnalysisResult.buildWithStatus(Status.EXCEPTION, exception.getClass().getSimpleName() + ": " + exception.getMessage());
		}else{
			Store_ store = null;
			
			if(storeState.exists() && storeState.get() instanceof Store_) {
				store = (Store_) storeState.get();
				
				if(store.hasExpired(metric.getInstant()))
					result = AnalysisResult.buildWithStatus(Status.EXCEPTION, exception.getClass().getSimpleName() + ": " + exception.getMessage());
			}else{
				store = new Store_();
				store.lastResult = metric.getInstant();
				
				result = AnalysisResult.buildWithStatus(Status.EXCEPTION, exception.getClass().getSimpleName() + ": " + exception.getMessage());
			}
			
			storeState.update(store);
		}
		
		if(result != null) {
			result.addMonitorParam("name", id);
			result.setTags(tags);
		}
		
		return Optional.ofNullable(result);
	}
	
	@Override
	public MetricsFilter getFilter() {
		if(filter != null)
			return filter;
		else
			return new MetricsFilter();
	}
	
	@Override
	public Map<String, Notificator> getNotificators() {
		Map<String, Notificator> notificators = new HashMap<>();
		
		ConstantNotificator notificator = new ConstantNotificator();
		Properties properties = new Properties();
		properties.put("statuses", "EXCEPTION");
		properties.put("period", "30m");
		try {
			notificator.config(properties);
		} catch (ConfigurationException e) {}
		notificators.put("monitor-in-error", notificator);
		
		return notificators;
	}
	
	private static class Store_ implements Store{

		private static final long serialVersionUID = -6991497562392525744L;

		private Instant lastResult;

		public boolean hasExpired(Instant instant) {
			if(lastResult == null) {
				lastResult = instant;
				return true;
			}
			
			if(lastResult.plus(Duration.ofMinutes(1)).compareTo(instant) <= 0) {
				lastResult = instant;
				
				return true;
			}else
				return false;
		}
		
	}
	
}
