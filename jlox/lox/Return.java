package jlox.lox;

/**
 * A Return object for return statements to move through multiple stack calls,
 * to call point.
 * 
 * Effectively uses exceptions for control flow.
 */
class Return extends RuntimeException {
  final Object value;

  Return(Object value) {
    super(null, null, false, false);
    this.value = value;
  }
}
