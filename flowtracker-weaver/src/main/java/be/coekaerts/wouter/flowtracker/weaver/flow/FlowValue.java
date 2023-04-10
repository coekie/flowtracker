package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.Types;
import java.util.Objects;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

class FlowValue extends BasicValue {

  /**
   * An uninitialized value.
   * @see BasicValue#UNINITIALIZED_VALUE
   */
  static final FlowValue UNINITIALIZED_VALUE = new FlowValue(null);

  /** A byte, boolean, char, short, or int value. */
  static final FlowValue INT_VALUE = new FlowValue(Type.INT_TYPE);

  /** A float value. */
  static final FlowValue FLOAT_VALUE = new FlowValue(Type.FLOAT_TYPE);

  /** A long value. */
  static final FlowValue LONG_VALUE = new FlowValue(Type.LONG_TYPE);

  /** A double value. */
  static final FlowValue DOUBLE_VALUE = new FlowValue(Type.DOUBLE_TYPE);

  /** An object or array reference value. */
  static final FlowValue REFERENCE_VALUE = new FlowValue(Types.OBJECT);

  FlowValue(Type type) {
    super(type);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o.getClass() != this.getClass()) {
      return false;
    } else {
      return Objects.equals(getType(), ((FlowValue) o).getType());
    }
  }

  @Override
  public int hashCode() {
    return getType() == null ? 0 : getType().hashCode();
  }
}
