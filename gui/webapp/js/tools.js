function stripText(x) {
  return x.normalize('NFD').replace(/[\u0300-\u036f]/g, "");
}