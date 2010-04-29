#!/usr/bin/ruby -w

class_name = ARGV[0]
op = ARGV[1]

puts("public class #{class_name} extends junit.framework.TestCase {")
(1..30).each() {
 |k|
 n = 1 << k
 puts(<<EOF
  public void test_#{n}() {
    for (long l = Integer.MIN_VALUE; l <= Integer.MAX_VALUE; ++l) {
      int n = (int) l;
      int q1 = n #{op} #{n};
      int q2 = (int) (l #{op} #{n}L);
      if (q1 != q2) {
        System.err.println("l=" + l + " q1=" + q1 + " q2=" + q2);
      }
    }
  }
EOF
 )
}
puts("}")
