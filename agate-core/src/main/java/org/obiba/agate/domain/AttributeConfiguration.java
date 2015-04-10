/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.agate.domain;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Specifications for an attribute.
 */
public class AttributeConfiguration implements Serializable {

  private static final long serialVersionUID = 4346306569488908166L;

  @NotNull
  private String name;

  private boolean required = false;

  private Type type = Type.STRING;

  /**
   * Enumerated possible values
   */
  @Nullable
  private List<String> values;

  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void setType(String type) {
    setType(Type.valueOf(type.toUpperCase()));
  }

  public Type getType() {
    return type;
  }

  public boolean hasValues() {
    return values != null && values.size() > 0;
  }

  @Nullable
  public List<String> getValues() {
    return values;
  }

  public void setValues(@Nullable List<String> values) {
    this.values = values;
  }

  public void addValue(String value) {
    if(values == null) values = Lists.newArrayList();
    if (!values.contains(value)) values.add(value);
  }

  public void deleteValue(String value) {
    if (values == null) return;
    values.remove(value);
  }

  @Override
  public final String toString() {
    return Objects.toStringHelper(this).omitNullValues().add("name", name) //
      .add("required", required) //
      .add("type", type) //
      .add("values", values).toString();
  }

  public enum Type {
    STRING, INTEGER, DECIMAL, BOOLEAN
  }
}
