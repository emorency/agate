package org.obiba.agate.assertj;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertionUtils {

  private AssertionUtils() {}

  public static <T> void areIterableFieldsEqualToEachOther(Iterable<T> actualCollection,
      Iterable<T> expectedCollection) {

    assertThat(actualCollection).hasSameSizeAs(expectedCollection);
    Iterator<T> actualIt = actualCollection.iterator();
    Iterator<T> expectedIt = expectedCollection.iterator();
    while(actualIt.hasNext()) {
      assertThat(actualIt.next()).isEqualToComparingFieldByField(expectedIt.next());
    }
  }

}
