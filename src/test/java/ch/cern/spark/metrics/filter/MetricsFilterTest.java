package ch.cern.spark.metrics.filter;

import static ch.cern.spark.metrics.MetricTest.Metric;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.cern.properties.ConfigurationException;
import ch.cern.properties.Properties;
import ch.cern.spark.metrics.Metric;

public class MetricsFilterTest {

    @Test
    public void filterOneID() throws ParseException {
        MetricsFilter filter = new MetricsFilter();
        filter.addAttributesPredicate("K1", "V1");

        Map<String, String> ids = new HashMap<>();
        ids.put("K1", "V1");
        Metric metric = new Metric(Instant.now(), 0, ids);
        Assert.assertTrue(filter.test(metric));

        ids.put("K1", "V2");
        metric = new Metric(Instant.now(), 0, ids);
        Assert.assertFalse(filter.test(metric));
    }

    @Test
    public void parseFilterWithHierarchy() throws ParseException, ConfigurationException {
        Properties props = new Properties();
        props.setProperty("expr", "K1.K11=V1");
        props.setProperty("attribute.K2.K21", "V2");
        props.setProperty("attribute.K2.K22", "V3");
        MetricsFilter filter = MetricsFilter.build(props);

        assertTrue(filter.test(Metric(0, 0, "K1.K11=V1", "K2.K21=V2", "K2.K22=V3")));
        assertTrue(filter.test(Metric(0, 0, "K1.K11=V1", "K2.K21=V2", "K2.K22=V3", "K4.K41=V4")));
        assertFalse(filter.test(Metric(0, 0, "K1.K11=Vnop", "K2.K21=V2", "K2.K22=V3")));
        assertFalse(filter.test(Metric(0, 0, "K1.K11=V1")));
        assertFalse(filter.test(Metric(0, 0, "K1.K11=V1", "K2.K21=V2")));
        assertFalse(filter.test(Metric(0, 0, "K1.K11=V1", "K2.K21=V2", "K2.K22=Vnop")));
        assertFalse(filter.test(Metric(0, 0, "K1.K11=V1", "K2.K21=Vnop")));
    }

    @Test
    public void shouldCompineExprAndAttributes() throws ConfigurationException, ParseException {
        Properties props = new Properties();
        props.setProperty("expr", "K1=V1");
        props.setProperty("attribute.K2", "V2");
        props.setProperty("attribute.K3", "V3");
        MetricsFilter filter = MetricsFilter.build(props);

        assertTrue(filter.test(Metric(0, 0, "K1=V1", "K2=V2", "K3=V3")));
        assertTrue(filter.test(Metric(0, 0, "K1=V1", "K2=V2", "K3=V3", "K4=V4")));
        assertFalse(filter.test(Metric(0, 0, "K1=Vnop", "K2=V2", "K3=V3")));
        assertFalse(filter.test(Metric(0, 0, "K1=V1")));
        assertFalse(filter.test(Metric(0, 0, "K1=V1", "K2=V2")));
        assertFalse(filter.test(Metric(0, 0, "K1=V1", "K2=V2", "K3=Vnop")));
        assertFalse(filter.test(Metric(0, 0, "K1=V1", "K2=Vnop")));
    }

    @Test
    public void filterMultiValue() throws ParseException, ConfigurationException {
        Properties props = new Properties();
        props.setProperty("attribute.K1", "\"V1\" \"V2\"");
        MetricsFilter filter = MetricsFilter.build(props);

        assertTrue(filter.test(Metric(0, 0, "K1=V1")));
        assertTrue(filter.test(Metric(0, 0, "K1=V2")));
        assertFalse(filter.test(Metric(0, 0, "K1=Vnop")));
    }
    
    @Test
    public void filterMultiValueSimpleQuotes() throws ParseException, ConfigurationException {
        Properties props = new Properties();
        props.setProperty("attribute.K1", "'V1' 'V2'");
        MetricsFilter filter = MetricsFilter.build(props);

        assertTrue(filter.test(Metric(0, 0, "K1=V1")));
        assertTrue(filter.test(Metric(0, 0, "K1=V2")));
        assertFalse(filter.test(Metric(0, 0, "K1=Vnop")));
    }
    
    @Test
    public void filterMultiValueNegated() throws ParseException, ConfigurationException {
        Properties props = new Properties();
        props.setProperty("attribute.K1", "!\"V1\" \"V2\"");
        MetricsFilter filter = MetricsFilter.build(props);

        assertFalse(filter.test(Metric(0, 0, "K1=V1")));
        assertFalse(filter.test(Metric(0, 0, "K1=V2")));
        assertTrue(filter.test(Metric(0, 0, "K1=Vnop")));
    }

    @Test
    public void filterSeveralIDs() throws ParseException {
        MetricsFilter filter = new MetricsFilter();
        filter.addAttributesPredicate("K1", "V1");
        filter.addAttributesPredicate("K2", "V2");

        Map<String, String> ids = new HashMap<>();
        ids.put("K1", "V1");
        ids.put("K2", "V2");
        Metric metric = new Metric(Instant.now(), 0, ids);
        Assert.assertTrue(filter.test(metric));

        ids.put("K1", "V1");
        ids.put("K1", "V2");
        metric = new Metric(Instant.now(), 0, ids);
        Assert.assertFalse(filter.test(metric));
    }

    @Test
    public void negateFilter() throws ParseException {
        MetricsFilter filter = new MetricsFilter();
        filter.addAttributesPredicate("K1", "V1");
        filter.addAttributesPredicate("K2", "!V2");
        filter.addAttributesPredicate("K3", "V3");

        Map<String, String> ids = new HashMap<>();
        ids.put("K1", "V1");
        ids.put("K2", "V2NOT");
        ids.put("K3", "V3");
        Metric metric = new Metric(Instant.now(), 0, ids);
        Assert.assertTrue(filter.test(metric));

        ids.put("K1", "V1");
        ids.put("K2", "V2");
        ids.put("K3", "V3");
        metric = new Metric(Instant.now(), 0, ids);
        Assert.assertFalse(filter.test(metric));

        ids.put("K1", "V1");
        // ids.put("K2", null);
        ids.put("K3", "V3");
        metric = new Metric(Instant.now(), 0, ids);
        Assert.assertFalse(filter.test(metric));
    }

    @Test
    public void filterActualValueNull() throws ParseException {
        MetricsFilter filter = new MetricsFilter();
        filter.addAttributesPredicate("K1", "V1");

        Map<String, String> ids = new HashMap<>();
        Metric metric = new Metric(Instant.now(), 0, ids);
        Assert.assertFalse(filter.test(metric));
    }

    @Test
    public void noAttributesFilter() {
        MetricsFilter filter = new MetricsFilter();

        Map<String, String> ids = new HashMap<>();
        ids.put("K1", "V1");
        ids.put("K1", "V2");
        Metric metric = new Metric(Instant.now(), 0, ids);
        Assert.assertTrue(filter.test(metric));
    }

    @Test
    public void filterRegex() throws ParseException {
        MetricsFilter filter = new MetricsFilter();
        filter.addAttributesPredicate("K1", "V[0-9]");
        filter.addAttributesPredicate("K2", "V.*");
        filter.addAttributesPredicate("K3", "!K.*"); // K3 cannot start with K

        Map<String, String> ids = new HashMap<>();
        ids.put("K1", "V5");
        ids.put("K2", "Vfoo");
        Metric metric = new Metric(Instant.now(), 0, ids);
        Assert.assertTrue(filter.test(metric));

        ids = new HashMap<>();
        ids.put("K1", "V5");
        ids.put("K2", "Vfoo");
        ids.put("K3", "Vfoo");
        metric = new Metric(Instant.now(), 0, ids);
        Assert.assertTrue(filter.test(metric));

        ids = new HashMap<>();
        ids.put("K1", "V5");
        ids.put("K2", "Vfoo");
        ids.put("K3", "Kfoo"); // cannot start with K
        metric = new Metric(Instant.now(), 0, ids);
        Assert.assertFalse(filter.test(metric));

        ids = new HashMap<>();
        ids.put("K1", "V2");
        ids.put("K2", "Vyes");
        metric = new Metric(Instant.now(), 0, ids);
        Assert.assertTrue(filter.test(metric));

        ids.put("K1", "V2");
        ids.put("K2", "Pno");
        metric = new Metric(Instant.now(), 0, ids);
        Assert.assertFalse(filter.test(metric));

        ids.put("K1", "Vno");
        ids.put("K2", "Vyes");
        Assert.assertFalse(filter.test(metric));

        ids.put("K1", "Vno");
        ids.put("K2", "NO");
        Assert.assertFalse(filter.test(metric));
    }

}
