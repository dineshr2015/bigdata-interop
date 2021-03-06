package com.google.cloud.hadoop.io.bigquery.mapred;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.RecordReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link BigQueryMapredRecordReader}.
 */
@RunWith(JUnit4.class)
public class BigQueryMapredRecordReaderTest {

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Mock private RecordReader<LongWritable, JsonObject> mockRecordReader;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @After public void tearDown() {
    verifyNoMoreInteractions(mockRecordReader);
  }

  @Test public void testClose() throws IOException, InterruptedException {
    BigQueryMapredRecordReader recordReader =
        new BigQueryMapredRecordReader(mockRecordReader, 0);
    doNothing().when(mockRecordReader).close();
    recordReader.close();
    verify(mockRecordReader).close();
  }

  @Test public void testCreateKeyValue() {
    BigQueryMapredRecordReader recordReader =
        new BigQueryMapredRecordReader(mockRecordReader, 0);

    LongWritable w = recordReader.createKey();
    assertNotNull(w);

    JsonObject json = recordReader.createValue();
    assertNotNull(json);
  }

  @Test public void testGetPos() throws IOException, InterruptedException {
    BigQueryMapredRecordReader recordReader =
        new BigQueryMapredRecordReader(mockRecordReader, 1);

    when(mockRecordReader.getProgress()).thenReturn(256.0F);
    float f = recordReader.getPos();
    assertEquals(256.0F, f, 0.000001F);

    verify(mockRecordReader).getProgress();
  }

  @Test public void testGetProgress() throws IOException, InterruptedException {
    BigQueryMapredRecordReader recordReader =
        new BigQueryMapredRecordReader(mockRecordReader, 0);

    // Happy-path is already tested by testGetPos

    when(mockRecordReader.getProgress()).thenThrow(new InterruptedException());
    expectedException.expect(IOException.class);
    try {
      recordReader.getProgress();
    } finally {
      verify(mockRecordReader).getProgress();
    }
  }

  @Test public void testNextData() throws IOException, InterruptedException {
    BigQueryMapredRecordReader recordReader =
        new BigQueryMapredRecordReader(mockRecordReader, 0);
    when(mockRecordReader.nextKeyValue()).thenReturn(true);
    when(mockRecordReader.getCurrentKey())
        .thenReturn(new LongWritable(123));
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("key1", "value1");
    when(mockRecordReader.getCurrentValue())
        .thenReturn(jsonObject);
    LongWritable key = new LongWritable(0);
    JsonObject value = new JsonObject();
    assertTrue(recordReader.next(key, value));
    assertEquals(new LongWritable(123), key);
    assertEquals(jsonObject, value);

    verify(mockRecordReader).nextKeyValue();
    verify(mockRecordReader).getCurrentKey();
    verify(mockRecordReader).getCurrentValue();

    when(mockRecordReader.nextKeyValue()).thenThrow(new InterruptedException());
    expectedException.expect(IOException.class);
    try {
      recordReader.next(key, value);
    } finally {
      verify(mockRecordReader, times(2)).nextKeyValue();
    }
  }

  @Test public void testNextEof() throws IOException, InterruptedException {
    BigQueryMapredRecordReader recordReader =
        new BigQueryMapredRecordReader(mockRecordReader, 0);
    when(mockRecordReader.nextKeyValue())
        .thenReturn(false);
    LongWritable key = new LongWritable(0);
    JsonObject value = new JsonObject();
    assertFalse(recordReader.next(key, value));

    verify(mockRecordReader).nextKeyValue();
  }

  @Test public void testCopyJsonObject() {
    JsonObject source = new JsonObject();
    source.addProperty("key1", "value1");
    source.addProperty("key2", 123);
    source.addProperty("key3", true);

    JsonObject destination = new JsonObject();
    destination.addProperty("key1", "different value");
    destination.addProperty("key4", "a value");

    assertTrue(destination.has("key4"));
    assertFalse(source.equals(destination));

    BigQueryMapredRecordReader recordReader =
        new BigQueryMapredRecordReader(null, 0);
    recordReader.copyJsonObject(source, destination);

    assertFalse(destination.has("key4"));
    assertTrue(source.equals(destination));
  }
}
