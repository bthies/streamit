import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class dummy extends Filter // Serpent.str:14
{
  public static dummy __construct()
  {
    dummy __obj = new dummy();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int[] out; // Serpent.str:16
  public void work() { // Serpent.str:17
    for (int i = 0; (i < 128); i++) { // Serpent.str:19
      outputChannel.pushInt(inputChannel.popInt()); // Serpent.str:19
    }; // Serpent.str:18
    outputChannel.pushInt(out[1]); // Serpent.str:20
  }
  public void init() { // Serpent.str:14
    int[] __sa7 = {128}; // Serpent.str:16
    out = ((int[])(ArrayMemoizer.initArray(new int[0], __sa7))); // Serpent.str:16
    setIOTypes(Integer.TYPE, Integer.TYPE); // Serpent.str:14
    addSteadyPhase(128, 128, (128 + 1), "work"); // Serpent.str:17
  }
}
class SerpentEncoder extends Pipeline // Serpent.str:24
{
  private int __param_vector;
  public static SerpentEncoder __construct(int vector)
  {
    SerpentEncoder __obj = new SerpentEncoder();
    __obj.__param_vector = vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector);
  }
  public void init(final int vector) { // Serpent.str:25
    add(Permute.__construct(TheGlobal.__get_instance().NBITS, TheGlobal.__get_instance().IP)); // Serpent.str:27
    for (int i = 0; (i < TheGlobal.__get_instance().MAXROUNDS); i++) { // Serpent.str:29
      add(R.__construct(vector, i)); // Serpent.str:31
    }; // Serpent.str:29
    add(Permute.__construct(TheGlobal.__get_instance().NBITS, TheGlobal.__get_instance().FP)); // Serpent.str:35
  }
}
class Sbox extends Filter // Serpent.str:40
{
  private int __param__param_round;
  public static Sbox __construct(int _param_round)
  {
    Sbox __obj = new Sbox();
    __obj.__param__param_round = _param_round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_round);
  }
  int round; // Serpent.str:40
  public void work() { // Serpent.str:42
    int val = inputChannel.popInt(); // Serpent.str:43
    val = ((inputChannel.popInt() << 1) | val); // Serpent.str:44
    val = ((inputChannel.popInt() << 2) | val); // Serpent.str:45
    val = ((inputChannel.popInt() << 3) | val); // Serpent.str:46
    int out = TheGlobal.__get_instance().SBOXES[round][val]; // Serpent.str:48
    outputChannel.pushInt(((int)(((out & 1) >> 0)))); // Serpent.str:49
    outputChannel.pushInt(((int)(((out & 2) >> 1)))); // Serpent.str:50
    outputChannel.pushInt(((int)(((out & 4) >> 2)))); // Serpent.str:51
    outputChannel.pushInt(((int)(((out & 8) >> 3)))); // Serpent.str:52
  }
  public void init(final int _param_round) { // Serpent.str:40
    round = _param_round; // Serpent.str:40
    setIOTypes(Integer.TYPE, Integer.TYPE); // Serpent.str:40
    addSteadyPhase(4, 4, 4, "work"); // Serpent.str:42
  }
}
class R extends Pipeline // Serpent.str:57
{
  private int __param_vector;
  private int __param_round;
  public static R __construct(int vector, int round)
  {
    R __obj = new R();
    __obj.__param_vector = vector;
    __obj.__param_round = round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_round);
  }
  public void init(final int vector, final int round) { // Serpent.str:58
    add(dummy.__construct()); // Serpent.str:59
    add(AnonFilter_a0.__construct(TheGlobal.__get_instance().NBITS, round, vector)); // Serpent.str:60
    add(Xor.__construct(2)); // Serpent.str:67
    add(Sbox.__construct((round % 8))); // Serpent.str:69
    if ((round < (TheGlobal.__get_instance().MAXROUNDS - 1))) { // Serpent.str:71
      add(rawL.__construct()); // Serpent.str:72
    } else { // Serpent.str:73
      add(dummy.__construct()); // Serpent.str:74
      add(AnonFilter_a1.__construct(TheGlobal.__get_instance().MAXROUNDS, TheGlobal.__get_instance().NBITS, vector)); // Serpent.str:75
      add(Xor.__construct(2)); // Serpent.str:81
    } // Serpent.str:71
  }
}
class PlainTextSource extends Pipeline // Serpent.str:90
{
  private int __param_vector;
  public static PlainTextSource __construct(int vector)
  {
    PlainTextSource __obj = new PlainTextSource();
    __obj.__param_vector = vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector);
  }
  public void init(final int vector) { // Serpent.str:91
    int[][] TEXT = {{0,0,0,0},
{3,2,1,0},
{-1829788726,-1804109491,838131579,-836508150},
{1091779113,-1182909630,827629196,755692158},
{1860758983,1305472976,-943804700,-875836990}
}; // Serpent.str:92
    add(RandomSource.__construct()); // Serpent.str:109
    add(IntoBits.__construct()); // Serpent.str:110
    if (TheGlobal.__get_instance().PRINTINFO) { // Serpent.str:112
      add(AnonFilter_a2.__construct(TheGlobal.__get_instance().PLAINTEXT)); // Serpent.str:113
    } // Serpent.str:112
  }
}
class RandomSource extends Filter // Serpent.str:122
{
  public static RandomSource __construct()
  {
    RandomSource __obj = new RandomSource();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int seed; // Serpent.str:123
  public void work() { // Serpent.str:124
    outputChannel.pushInt(seed); // Serpent.str:125
    seed = (((65793 * seed) + 4282663) % 8388608); // Serpent.str:126
  }
  public void init() { // Serpent.str:122
    seed = 0; // Serpent.str:123
    setIOTypes(Void.TYPE, Integer.TYPE); // Serpent.str:122
    addSteadyPhase(0, 0, 1, "work"); // Serpent.str:124
  }
}
class KeySchedule extends Pipeline // Serpent.str:137
{
  private int __param_vector;
  private int __param_round;
  public static KeySchedule __construct(int vector, int round)
  {
    KeySchedule __obj = new KeySchedule();
    __obj.__param_vector = vector;
    __obj.__param_round = round;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_vector, __param_round);
  }
  public void init(final int vector, final int round) { // Serpent.str:138
    add(AnonFilter_a3.__construct(TheGlobal.__get_instance().BITS_PER_WORD, 128, TheGlobal.__get_instance().IP, TheGlobal.__get_instance().MAXROUNDS, TheGlobal.__get_instance().NBITS, TheGlobal.__get_instance().PHI, 8, 16, TheGlobal.__get_instance().SBOXES, 5, 8, TheGlobal.__get_instance().USERKEYS, TheGlobal.__get_instance().USERKEY_LENGTH, round, vector)); // Serpent.str:139
  }
}
class rawL extends Filter // Serpent.str:253
{
  public static rawL __construct()
  {
    rawL __obj = new rawL();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // Serpent.str:255
    outputChannel.pushInt(((((((inputChannel.peekInt(16) ^ inputChannel.peekInt(52)) ^ inputChannel.peekInt(56)) ^ inputChannel.peekInt(70)) ^ inputChannel.peekInt(83)) ^ inputChannel.peekInt(94)) ^ inputChannel.peekInt(105))); // Serpent.str:256
    outputChannel.pushInt(((inputChannel.peekInt(72) ^ inputChannel.peekInt(114)) ^ inputChannel.peekInt(125))); // Serpent.str:257
    outputChannel.pushInt(((((((inputChannel.peekInt(2) ^ inputChannel.peekInt(9)) ^ inputChannel.peekInt(15)) ^ inputChannel.peekInt(30)) ^ inputChannel.peekInt(76)) ^ inputChannel.peekInt(84)) ^ inputChannel.peekInt(126))); // Serpent.str:258
    outputChannel.pushInt(((inputChannel.peekInt(36) ^ inputChannel.peekInt(90)) ^ inputChannel.peekInt(103))); // Serpent.str:259
    outputChannel.pushInt(((((((inputChannel.peekInt(20) ^ inputChannel.peekInt(56)) ^ inputChannel.peekInt(60)) ^ inputChannel.peekInt(74)) ^ inputChannel.peekInt(87)) ^ inputChannel.peekInt(98)) ^ inputChannel.peekInt(109))); // Serpent.str:260
    outputChannel.pushInt(((inputChannel.peekInt(1) ^ inputChannel.peekInt(76)) ^ inputChannel.peekInt(118))); // Serpent.str:261
    outputChannel.pushInt(((((((inputChannel.peekInt(2) ^ inputChannel.peekInt(6)) ^ inputChannel.peekInt(13)) ^ inputChannel.peekInt(19)) ^ inputChannel.peekInt(34)) ^ inputChannel.peekInt(80)) ^ inputChannel.peekInt(88))); // Serpent.str:262
    outputChannel.pushInt(((inputChannel.peekInt(40) ^ inputChannel.peekInt(94)) ^ inputChannel.peekInt(107))); // Serpent.str:263
    outputChannel.pushInt(((((((inputChannel.peekInt(24) ^ inputChannel.peekInt(60)) ^ inputChannel.peekInt(64)) ^ inputChannel.peekInt(78)) ^ inputChannel.peekInt(91)) ^ inputChannel.peekInt(102)) ^ inputChannel.peekInt(113))); // Serpent.str:264
    outputChannel.pushInt(((inputChannel.peekInt(5) ^ inputChannel.peekInt(80)) ^ inputChannel.peekInt(122))); // Serpent.str:265
    outputChannel.pushInt(((((((inputChannel.peekInt(6) ^ inputChannel.peekInt(10)) ^ inputChannel.peekInt(17)) ^ inputChannel.peekInt(23)) ^ inputChannel.peekInt(38)) ^ inputChannel.peekInt(84)) ^ inputChannel.peekInt(92))); // Serpent.str:266
    outputChannel.pushInt(((inputChannel.peekInt(44) ^ inputChannel.peekInt(98)) ^ inputChannel.peekInt(111))); // Serpent.str:267
    outputChannel.pushInt(((((((inputChannel.peekInt(28) ^ inputChannel.peekInt(64)) ^ inputChannel.peekInt(68)) ^ inputChannel.peekInt(82)) ^ inputChannel.peekInt(95)) ^ inputChannel.peekInt(106)) ^ inputChannel.peekInt(117))); // Serpent.str:268
    outputChannel.pushInt(((inputChannel.peekInt(9) ^ inputChannel.peekInt(84)) ^ inputChannel.peekInt(126))); // Serpent.str:269
    outputChannel.pushInt(((((((inputChannel.peekInt(10) ^ inputChannel.peekInt(14)) ^ inputChannel.peekInt(21)) ^ inputChannel.peekInt(27)) ^ inputChannel.peekInt(42)) ^ inputChannel.peekInt(88)) ^ inputChannel.peekInt(96))); // Serpent.str:270
    outputChannel.pushInt(((inputChannel.peekInt(48) ^ inputChannel.peekInt(102)) ^ inputChannel.peekInt(115))); // Serpent.str:271
    outputChannel.pushInt(((((((inputChannel.peekInt(32) ^ inputChannel.peekInt(68)) ^ inputChannel.peekInt(72)) ^ inputChannel.peekInt(86)) ^ inputChannel.peekInt(99)) ^ inputChannel.peekInt(110)) ^ inputChannel.peekInt(121))); // Serpent.str:272
    outputChannel.pushInt(((inputChannel.peekInt(2) ^ inputChannel.peekInt(13)) ^ inputChannel.peekInt(88))); // Serpent.str:273
    outputChannel.pushInt(((((((inputChannel.peekInt(14) ^ inputChannel.peekInt(18)) ^ inputChannel.peekInt(25)) ^ inputChannel.peekInt(31)) ^ inputChannel.peekInt(46)) ^ inputChannel.peekInt(92)) ^ inputChannel.peekInt(100))); // Serpent.str:274
    outputChannel.pushInt(((inputChannel.peekInt(52) ^ inputChannel.peekInt(106)) ^ inputChannel.peekInt(119))); // Serpent.str:275
    outputChannel.pushInt(((((((inputChannel.peekInt(36) ^ inputChannel.peekInt(72)) ^ inputChannel.peekInt(76)) ^ inputChannel.peekInt(90)) ^ inputChannel.peekInt(103)) ^ inputChannel.peekInt(114)) ^ inputChannel.peekInt(125))); // Serpent.str:276
    outputChannel.pushInt(((inputChannel.peekInt(6) ^ inputChannel.peekInt(17)) ^ inputChannel.peekInt(92))); // Serpent.str:277
    outputChannel.pushInt(((((((inputChannel.peekInt(18) ^ inputChannel.peekInt(22)) ^ inputChannel.peekInt(29)) ^ inputChannel.peekInt(35)) ^ inputChannel.peekInt(50)) ^ inputChannel.peekInt(96)) ^ inputChannel.peekInt(104))); // Serpent.str:278
    outputChannel.pushInt(((inputChannel.peekInt(56) ^ inputChannel.peekInt(110)) ^ inputChannel.peekInt(123))); // Serpent.str:279
    outputChannel.pushInt(((((((inputChannel.peekInt(1) ^ inputChannel.peekInt(40)) ^ inputChannel.peekInt(76)) ^ inputChannel.peekInt(80)) ^ inputChannel.peekInt(94)) ^ inputChannel.peekInt(107)) ^ inputChannel.peekInt(118))); // Serpent.str:280
    outputChannel.pushInt(((inputChannel.peekInt(10) ^ inputChannel.peekInt(21)) ^ inputChannel.peekInt(96))); // Serpent.str:281
    outputChannel.pushInt(((((((inputChannel.peekInt(22) ^ inputChannel.peekInt(26)) ^ inputChannel.peekInt(33)) ^ inputChannel.peekInt(39)) ^ inputChannel.peekInt(54)) ^ inputChannel.peekInt(100)) ^ inputChannel.peekInt(108))); // Serpent.str:282
    outputChannel.pushInt(((inputChannel.peekInt(60) ^ inputChannel.peekInt(114)) ^ inputChannel.peekInt(127))); // Serpent.str:283
    outputChannel.pushInt(((((((inputChannel.peekInt(5) ^ inputChannel.peekInt(44)) ^ inputChannel.peekInt(80)) ^ inputChannel.peekInt(84)) ^ inputChannel.peekInt(98)) ^ inputChannel.peekInt(111)) ^ inputChannel.peekInt(122))); // Serpent.str:284
    outputChannel.pushInt(((inputChannel.peekInt(14) ^ inputChannel.peekInt(25)) ^ inputChannel.peekInt(100))); // Serpent.str:285
    outputChannel.pushInt(((((((inputChannel.peekInt(26) ^ inputChannel.peekInt(30)) ^ inputChannel.peekInt(37)) ^ inputChannel.peekInt(43)) ^ inputChannel.peekInt(58)) ^ inputChannel.peekInt(104)) ^ inputChannel.peekInt(112))); // Serpent.str:286
    outputChannel.pushInt((inputChannel.peekInt(3) ^ inputChannel.peekInt(118))); // Serpent.str:287
    outputChannel.pushInt(((((((inputChannel.peekInt(9) ^ inputChannel.peekInt(48)) ^ inputChannel.peekInt(84)) ^ inputChannel.peekInt(88)) ^ inputChannel.peekInt(102)) ^ inputChannel.peekInt(115)) ^ inputChannel.peekInt(126))); // Serpent.str:288
    outputChannel.pushInt(((inputChannel.peekInt(18) ^ inputChannel.peekInt(29)) ^ inputChannel.peekInt(104))); // Serpent.str:289
    outputChannel.pushInt(((((((inputChannel.peekInt(30) ^ inputChannel.peekInt(34)) ^ inputChannel.peekInt(41)) ^ inputChannel.peekInt(47)) ^ inputChannel.peekInt(62)) ^ inputChannel.peekInt(108)) ^ inputChannel.peekInt(116))); // Serpent.str:290
    outputChannel.pushInt((inputChannel.peekInt(7) ^ inputChannel.peekInt(122))); // Serpent.str:291
    outputChannel.pushInt(((((((inputChannel.peekInt(2) ^ inputChannel.peekInt(13)) ^ inputChannel.peekInt(52)) ^ inputChannel.peekInt(88)) ^ inputChannel.peekInt(92)) ^ inputChannel.peekInt(106)) ^ inputChannel.peekInt(119))); // Serpent.str:292
    outputChannel.pushInt(((inputChannel.peekInt(22) ^ inputChannel.peekInt(33)) ^ inputChannel.peekInt(108))); // Serpent.str:293
    outputChannel.pushInt(((((((inputChannel.peekInt(34) ^ inputChannel.peekInt(38)) ^ inputChannel.peekInt(45)) ^ inputChannel.peekInt(51)) ^ inputChannel.peekInt(66)) ^ inputChannel.peekInt(112)) ^ inputChannel.peekInt(120))); // Serpent.str:294
    outputChannel.pushInt((inputChannel.peekInt(11) ^ inputChannel.peekInt(126))); // Serpent.str:295
    outputChannel.pushInt(((((((inputChannel.peekInt(6) ^ inputChannel.peekInt(17)) ^ inputChannel.peekInt(56)) ^ inputChannel.peekInt(92)) ^ inputChannel.peekInt(96)) ^ inputChannel.peekInt(110)) ^ inputChannel.peekInt(123))); // Serpent.str:296
    outputChannel.pushInt(((inputChannel.peekInt(26) ^ inputChannel.peekInt(37)) ^ inputChannel.peekInt(112))); // Serpent.str:297
    outputChannel.pushInt(((((((inputChannel.peekInt(38) ^ inputChannel.peekInt(42)) ^ inputChannel.peekInt(49)) ^ inputChannel.peekInt(55)) ^ inputChannel.peekInt(70)) ^ inputChannel.peekInt(116)) ^ inputChannel.peekInt(124))); // Serpent.str:298
    outputChannel.pushInt(((inputChannel.peekInt(2) ^ inputChannel.peekInt(15)) ^ inputChannel.peekInt(76))); // Serpent.str:299
    outputChannel.pushInt(((((((inputChannel.peekInt(10) ^ inputChannel.peekInt(21)) ^ inputChannel.peekInt(60)) ^ inputChannel.peekInt(96)) ^ inputChannel.peekInt(100)) ^ inputChannel.peekInt(114)) ^ inputChannel.peekInt(127))); // Serpent.str:300
    outputChannel.pushInt(((inputChannel.peekInt(30) ^ inputChannel.peekInt(41)) ^ inputChannel.peekInt(116))); // Serpent.str:301
    outputChannel.pushInt(((((((inputChannel.peekInt(0) ^ inputChannel.peekInt(42)) ^ inputChannel.peekInt(46)) ^ inputChannel.peekInt(53)) ^ inputChannel.peekInt(59)) ^ inputChannel.peekInt(74)) ^ inputChannel.peekInt(120))); // Serpent.str:302
    outputChannel.pushInt(((inputChannel.peekInt(6) ^ inputChannel.peekInt(19)) ^ inputChannel.peekInt(80))); // Serpent.str:303
    outputChannel.pushInt((((((inputChannel.peekInt(3) ^ inputChannel.peekInt(14)) ^ inputChannel.peekInt(25)) ^ inputChannel.peekInt(100)) ^ inputChannel.peekInt(104)) ^ inputChannel.peekInt(118))); // Serpent.str:304
    outputChannel.pushInt(((inputChannel.peekInt(34) ^ inputChannel.peekInt(45)) ^ inputChannel.peekInt(120))); // Serpent.str:305
    outputChannel.pushInt(((((((inputChannel.peekInt(4) ^ inputChannel.peekInt(46)) ^ inputChannel.peekInt(50)) ^ inputChannel.peekInt(57)) ^ inputChannel.peekInt(63)) ^ inputChannel.peekInt(78)) ^ inputChannel.peekInt(124))); // Serpent.str:306
    outputChannel.pushInt(((inputChannel.peekInt(10) ^ inputChannel.peekInt(23)) ^ inputChannel.peekInt(84))); // Serpent.str:307
    outputChannel.pushInt((((((inputChannel.peekInt(7) ^ inputChannel.peekInt(18)) ^ inputChannel.peekInt(29)) ^ inputChannel.peekInt(104)) ^ inputChannel.peekInt(108)) ^ inputChannel.peekInt(122))); // Serpent.str:308
    outputChannel.pushInt(((inputChannel.peekInt(38) ^ inputChannel.peekInt(49)) ^ inputChannel.peekInt(124))); // Serpent.str:309
    outputChannel.pushInt(((((((inputChannel.peekInt(0) ^ inputChannel.peekInt(8)) ^ inputChannel.peekInt(50)) ^ inputChannel.peekInt(54)) ^ inputChannel.peekInt(61)) ^ inputChannel.peekInt(67)) ^ inputChannel.peekInt(82))); // Serpent.str:310
    outputChannel.pushInt(((inputChannel.peekInt(14) ^ inputChannel.peekInt(27)) ^ inputChannel.peekInt(88))); // Serpent.str:311
    outputChannel.pushInt((((((inputChannel.peekInt(11) ^ inputChannel.peekInt(22)) ^ inputChannel.peekInt(33)) ^ inputChannel.peekInt(108)) ^ inputChannel.peekInt(112)) ^ inputChannel.peekInt(126))); // Serpent.str:312
    outputChannel.pushInt(((inputChannel.peekInt(0) ^ inputChannel.peekInt(42)) ^ inputChannel.peekInt(53))); // Serpent.str:313
    outputChannel.pushInt(((((((inputChannel.peekInt(4) ^ inputChannel.peekInt(12)) ^ inputChannel.peekInt(54)) ^ inputChannel.peekInt(58)) ^ inputChannel.peekInt(65)) ^ inputChannel.peekInt(71)) ^ inputChannel.peekInt(86))); // Serpent.str:314
    outputChannel.pushInt(((inputChannel.peekInt(18) ^ inputChannel.peekInt(31)) ^ inputChannel.peekInt(92))); // Serpent.str:315
    outputChannel.pushInt(((((((inputChannel.peekInt(2) ^ inputChannel.peekInt(15)) ^ inputChannel.peekInt(26)) ^ inputChannel.peekInt(37)) ^ inputChannel.peekInt(76)) ^ inputChannel.peekInt(112)) ^ inputChannel.peekInt(116))); // Serpent.str:316
    outputChannel.pushInt(((inputChannel.peekInt(4) ^ inputChannel.peekInt(46)) ^ inputChannel.peekInt(57))); // Serpent.str:317
    outputChannel.pushInt(((((((inputChannel.peekInt(8) ^ inputChannel.peekInt(16)) ^ inputChannel.peekInt(58)) ^ inputChannel.peekInt(62)) ^ inputChannel.peekInt(69)) ^ inputChannel.peekInt(75)) ^ inputChannel.peekInt(90))); // Serpent.str:318
    outputChannel.pushInt(((inputChannel.peekInt(22) ^ inputChannel.peekInt(35)) ^ inputChannel.peekInt(96))); // Serpent.str:319
    outputChannel.pushInt(((((((inputChannel.peekInt(6) ^ inputChannel.peekInt(19)) ^ inputChannel.peekInt(30)) ^ inputChannel.peekInt(41)) ^ inputChannel.peekInt(80)) ^ inputChannel.peekInt(116)) ^ inputChannel.peekInt(120))); // Serpent.str:320
    outputChannel.pushInt(((inputChannel.peekInt(8) ^ inputChannel.peekInt(50)) ^ inputChannel.peekInt(61))); // Serpent.str:321
    outputChannel.pushInt(((((((inputChannel.peekInt(12) ^ inputChannel.peekInt(20)) ^ inputChannel.peekInt(62)) ^ inputChannel.peekInt(66)) ^ inputChannel.peekInt(73)) ^ inputChannel.peekInt(79)) ^ inputChannel.peekInt(94))); // Serpent.str:322
    outputChannel.pushInt(((inputChannel.peekInt(26) ^ inputChannel.peekInt(39)) ^ inputChannel.peekInt(100))); // Serpent.str:323
    outputChannel.pushInt(((((((inputChannel.peekInt(10) ^ inputChannel.peekInt(23)) ^ inputChannel.peekInt(34)) ^ inputChannel.peekInt(45)) ^ inputChannel.peekInt(84)) ^ inputChannel.peekInt(120)) ^ inputChannel.peekInt(124))); // Serpent.str:324
    outputChannel.pushInt(((inputChannel.peekInt(12) ^ inputChannel.peekInt(54)) ^ inputChannel.peekInt(65))); // Serpent.str:325
    outputChannel.pushInt(((((((inputChannel.peekInt(16) ^ inputChannel.peekInt(24)) ^ inputChannel.peekInt(66)) ^ inputChannel.peekInt(70)) ^ inputChannel.peekInt(77)) ^ inputChannel.peekInt(83)) ^ inputChannel.peekInt(98))); // Serpent.str:326
    outputChannel.pushInt(((inputChannel.peekInt(30) ^ inputChannel.peekInt(43)) ^ inputChannel.peekInt(104))); // Serpent.str:327
    outputChannel.pushInt(((((((inputChannel.peekInt(0) ^ inputChannel.peekInt(14)) ^ inputChannel.peekInt(27)) ^ inputChannel.peekInt(38)) ^ inputChannel.peekInt(49)) ^ inputChannel.peekInt(88)) ^ inputChannel.peekInt(124))); // Serpent.str:328
    outputChannel.pushInt(((inputChannel.peekInt(16) ^ inputChannel.peekInt(58)) ^ inputChannel.peekInt(69))); // Serpent.str:329
    outputChannel.pushInt(((((((inputChannel.peekInt(20) ^ inputChannel.peekInt(28)) ^ inputChannel.peekInt(70)) ^ inputChannel.peekInt(74)) ^ inputChannel.peekInt(81)) ^ inputChannel.peekInt(87)) ^ inputChannel.peekInt(102))); // Serpent.str:330
    outputChannel.pushInt(((inputChannel.peekInt(34) ^ inputChannel.peekInt(47)) ^ inputChannel.peekInt(108))); // Serpent.str:331
    outputChannel.pushInt(((((((inputChannel.peekInt(0) ^ inputChannel.peekInt(4)) ^ inputChannel.peekInt(18)) ^ inputChannel.peekInt(31)) ^ inputChannel.peekInt(42)) ^ inputChannel.peekInt(53)) ^ inputChannel.peekInt(92))); // Serpent.str:332
    outputChannel.pushInt(((inputChannel.peekInt(20) ^ inputChannel.peekInt(62)) ^ inputChannel.peekInt(73))); // Serpent.str:333
    outputChannel.pushInt(((((((inputChannel.peekInt(24) ^ inputChannel.peekInt(32)) ^ inputChannel.peekInt(74)) ^ inputChannel.peekInt(78)) ^ inputChannel.peekInt(85)) ^ inputChannel.peekInt(91)) ^ inputChannel.peekInt(106))); // Serpent.str:334
    outputChannel.pushInt(((inputChannel.peekInt(38) ^ inputChannel.peekInt(51)) ^ inputChannel.peekInt(112))); // Serpent.str:335
    outputChannel.pushInt(((((((inputChannel.peekInt(4) ^ inputChannel.peekInt(8)) ^ inputChannel.peekInt(22)) ^ inputChannel.peekInt(35)) ^ inputChannel.peekInt(46)) ^ inputChannel.peekInt(57)) ^ inputChannel.peekInt(96))); // Serpent.str:336
    outputChannel.pushInt(((inputChannel.peekInt(24) ^ inputChannel.peekInt(66)) ^ inputChannel.peekInt(77))); // Serpent.str:337
    outputChannel.pushInt(((((((inputChannel.peekInt(28) ^ inputChannel.peekInt(36)) ^ inputChannel.peekInt(78)) ^ inputChannel.peekInt(82)) ^ inputChannel.peekInt(89)) ^ inputChannel.peekInt(95)) ^ inputChannel.peekInt(110))); // Serpent.str:338
    outputChannel.pushInt(((inputChannel.peekInt(42) ^ inputChannel.peekInt(55)) ^ inputChannel.peekInt(116))); // Serpent.str:339
    outputChannel.pushInt(((((((inputChannel.peekInt(8) ^ inputChannel.peekInt(12)) ^ inputChannel.peekInt(26)) ^ inputChannel.peekInt(39)) ^ inputChannel.peekInt(50)) ^ inputChannel.peekInt(61)) ^ inputChannel.peekInt(100))); // Serpent.str:340
    outputChannel.pushInt(((inputChannel.peekInt(28) ^ inputChannel.peekInt(70)) ^ inputChannel.peekInt(81))); // Serpent.str:341
    outputChannel.pushInt(((((((inputChannel.peekInt(32) ^ inputChannel.peekInt(40)) ^ inputChannel.peekInt(82)) ^ inputChannel.peekInt(86)) ^ inputChannel.peekInt(93)) ^ inputChannel.peekInt(99)) ^ inputChannel.peekInt(114))); // Serpent.str:342
    outputChannel.pushInt(((inputChannel.peekInt(46) ^ inputChannel.peekInt(59)) ^ inputChannel.peekInt(120))); // Serpent.str:343
    outputChannel.pushInt(((((((inputChannel.peekInt(12) ^ inputChannel.peekInt(16)) ^ inputChannel.peekInt(30)) ^ inputChannel.peekInt(43)) ^ inputChannel.peekInt(54)) ^ inputChannel.peekInt(65)) ^ inputChannel.peekInt(104))); // Serpent.str:344
    outputChannel.pushInt(((inputChannel.peekInt(32) ^ inputChannel.peekInt(74)) ^ inputChannel.peekInt(85))); // Serpent.str:345
    outputChannel.pushInt((((inputChannel.peekInt(36) ^ inputChannel.peekInt(90)) ^ inputChannel.peekInt(103)) ^ inputChannel.peekInt(118))); // Serpent.str:346
    outputChannel.pushInt(((inputChannel.peekInt(50) ^ inputChannel.peekInt(63)) ^ inputChannel.peekInt(124))); // Serpent.str:347
    outputChannel.pushInt(((((((inputChannel.peekInt(16) ^ inputChannel.peekInt(20)) ^ inputChannel.peekInt(34)) ^ inputChannel.peekInt(47)) ^ inputChannel.peekInt(58)) ^ inputChannel.peekInt(69)) ^ inputChannel.peekInt(108))); // Serpent.str:348
    outputChannel.pushInt(((inputChannel.peekInt(36) ^ inputChannel.peekInt(78)) ^ inputChannel.peekInt(89))); // Serpent.str:349
    outputChannel.pushInt((((inputChannel.peekInt(40) ^ inputChannel.peekInt(94)) ^ inputChannel.peekInt(107)) ^ inputChannel.peekInt(122))); // Serpent.str:350
    outputChannel.pushInt(((inputChannel.peekInt(0) ^ inputChannel.peekInt(54)) ^ inputChannel.peekInt(67))); // Serpent.str:351
    outputChannel.pushInt(((((((inputChannel.peekInt(20) ^ inputChannel.peekInt(24)) ^ inputChannel.peekInt(38)) ^ inputChannel.peekInt(51)) ^ inputChannel.peekInt(62)) ^ inputChannel.peekInt(73)) ^ inputChannel.peekInt(112))); // Serpent.str:352
    outputChannel.pushInt(((inputChannel.peekInt(40) ^ inputChannel.peekInt(82)) ^ inputChannel.peekInt(93))); // Serpent.str:353
    outputChannel.pushInt((((inputChannel.peekInt(44) ^ inputChannel.peekInt(98)) ^ inputChannel.peekInt(111)) ^ inputChannel.peekInt(126))); // Serpent.str:354
    outputChannel.pushInt(((inputChannel.peekInt(4) ^ inputChannel.peekInt(58)) ^ inputChannel.peekInt(71))); // Serpent.str:355
    outputChannel.pushInt(((((((inputChannel.peekInt(24) ^ inputChannel.peekInt(28)) ^ inputChannel.peekInt(42)) ^ inputChannel.peekInt(55)) ^ inputChannel.peekInt(66)) ^ inputChannel.peekInt(77)) ^ inputChannel.peekInt(116))); // Serpent.str:356
    outputChannel.pushInt(((inputChannel.peekInt(44) ^ inputChannel.peekInt(86)) ^ inputChannel.peekInt(97))); // Serpent.str:357
    outputChannel.pushInt((((inputChannel.peekInt(2) ^ inputChannel.peekInt(48)) ^ inputChannel.peekInt(102)) ^ inputChannel.peekInt(115))); // Serpent.str:358
    outputChannel.pushInt(((inputChannel.peekInt(8) ^ inputChannel.peekInt(62)) ^ inputChannel.peekInt(75))); // Serpent.str:359
    outputChannel.pushInt(((((((inputChannel.peekInt(28) ^ inputChannel.peekInt(32)) ^ inputChannel.peekInt(46)) ^ inputChannel.peekInt(59)) ^ inputChannel.peekInt(70)) ^ inputChannel.peekInt(81)) ^ inputChannel.peekInt(120))); // Serpent.str:360
    outputChannel.pushInt(((inputChannel.peekInt(48) ^ inputChannel.peekInt(90)) ^ inputChannel.peekInt(101))); // Serpent.str:361
    outputChannel.pushInt((((inputChannel.peekInt(6) ^ inputChannel.peekInt(52)) ^ inputChannel.peekInt(106)) ^ inputChannel.peekInt(119))); // Serpent.str:362
    outputChannel.pushInt(((inputChannel.peekInt(12) ^ inputChannel.peekInt(66)) ^ inputChannel.peekInt(79))); // Serpent.str:363
    outputChannel.pushInt(((((((inputChannel.peekInt(32) ^ inputChannel.peekInt(36)) ^ inputChannel.peekInt(50)) ^ inputChannel.peekInt(63)) ^ inputChannel.peekInt(74)) ^ inputChannel.peekInt(85)) ^ inputChannel.peekInt(124))); // Serpent.str:364
    outputChannel.pushInt(((inputChannel.peekInt(52) ^ inputChannel.peekInt(94)) ^ inputChannel.peekInt(105))); // Serpent.str:365
    outputChannel.pushInt((((inputChannel.peekInt(10) ^ inputChannel.peekInt(56)) ^ inputChannel.peekInt(110)) ^ inputChannel.peekInt(123))); // Serpent.str:366
    outputChannel.pushInt(((inputChannel.peekInt(16) ^ inputChannel.peekInt(70)) ^ inputChannel.peekInt(83))); // Serpent.str:367
    outputChannel.pushInt(((((((inputChannel.peekInt(0) ^ inputChannel.peekInt(36)) ^ inputChannel.peekInt(40)) ^ inputChannel.peekInt(54)) ^ inputChannel.peekInt(67)) ^ inputChannel.peekInt(78)) ^ inputChannel.peekInt(89))); // Serpent.str:368
    outputChannel.pushInt(((inputChannel.peekInt(56) ^ inputChannel.peekInt(98)) ^ inputChannel.peekInt(109))); // Serpent.str:369
    outputChannel.pushInt((((inputChannel.peekInt(14) ^ inputChannel.peekInt(60)) ^ inputChannel.peekInt(114)) ^ inputChannel.peekInt(127))); // Serpent.str:370
    outputChannel.pushInt(((inputChannel.peekInt(20) ^ inputChannel.peekInt(74)) ^ inputChannel.peekInt(87))); // Serpent.str:371
    outputChannel.pushInt(((((((inputChannel.peekInt(4) ^ inputChannel.peekInt(40)) ^ inputChannel.peekInt(44)) ^ inputChannel.peekInt(58)) ^ inputChannel.peekInt(71)) ^ inputChannel.peekInt(82)) ^ inputChannel.peekInt(93))); // Serpent.str:372
    outputChannel.pushInt(((inputChannel.peekInt(60) ^ inputChannel.peekInt(102)) ^ inputChannel.peekInt(113))); // Serpent.str:373
    outputChannel.pushInt((((((inputChannel.peekInt(3) ^ inputChannel.peekInt(18)) ^ inputChannel.peekInt(72)) ^ inputChannel.peekInt(114)) ^ inputChannel.peekInt(118)) ^ inputChannel.peekInt(125))); // Serpent.str:374
    outputChannel.pushInt(((inputChannel.peekInt(24) ^ inputChannel.peekInt(78)) ^ inputChannel.peekInt(91))); // Serpent.str:375
    outputChannel.pushInt(((((((inputChannel.peekInt(8) ^ inputChannel.peekInt(44)) ^ inputChannel.peekInt(48)) ^ inputChannel.peekInt(62)) ^ inputChannel.peekInt(75)) ^ inputChannel.peekInt(86)) ^ inputChannel.peekInt(97))); // Serpent.str:376
    outputChannel.pushInt(((inputChannel.peekInt(64) ^ inputChannel.peekInt(106)) ^ inputChannel.peekInt(117))); // Serpent.str:377
    outputChannel.pushInt((((((inputChannel.peekInt(1) ^ inputChannel.peekInt(7)) ^ inputChannel.peekInt(22)) ^ inputChannel.peekInt(76)) ^ inputChannel.peekInt(118)) ^ inputChannel.peekInt(122))); // Serpent.str:378
    outputChannel.pushInt(((inputChannel.peekInt(28) ^ inputChannel.peekInt(82)) ^ inputChannel.peekInt(95))); // Serpent.str:379
    outputChannel.pushInt(((((((inputChannel.peekInt(12) ^ inputChannel.peekInt(48)) ^ inputChannel.peekInt(52)) ^ inputChannel.peekInt(66)) ^ inputChannel.peekInt(79)) ^ inputChannel.peekInt(90)) ^ inputChannel.peekInt(101))); // Serpent.str:380
    outputChannel.pushInt(((inputChannel.peekInt(68) ^ inputChannel.peekInt(110)) ^ inputChannel.peekInt(121))); // Serpent.str:381
    outputChannel.pushInt((((((inputChannel.peekInt(5) ^ inputChannel.peekInt(11)) ^ inputChannel.peekInt(26)) ^ inputChannel.peekInt(80)) ^ inputChannel.peekInt(122)) ^ inputChannel.peekInt(126))); // Serpent.str:382
    outputChannel.pushInt(((inputChannel.peekInt(32) ^ inputChannel.peekInt(86)) ^ inputChannel.peekInt(99))); // Serpent.str:383
    for (int i = 0; (i < 128); i++) { // Serpent.str:384
      inputChannel.popInt(); // Serpent.str:385
    }; // Serpent.str:384
  }
  public void init() { // Serpent.str:253
    setIOTypes(Integer.TYPE, Integer.TYPE); // Serpent.str:253
    addSteadyPhase(128, 128, 128, "work"); // Serpent.str:255
  }
}
class Permute extends Filter // Serpent.str:396
{
  private int __param__param_N;
  private int[] __param__param_permutation;
  public static Permute __construct(int _param_N, int[] _param_permutation)
  {
    Permute __obj = new Permute();
    __obj.__param__param_N = _param_N;
    __obj.__param__param_permutation = _param_permutation;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_N, __param__param_permutation);
  }
  int N; // Serpent.str:396
  int[] permutation; // Serpent.str:396
  public void work() { // Serpent.str:398
    for (int i = 0; (i < N); i++) { // Serpent.str:399
      outputChannel.pushInt(inputChannel.peekInt(permutation[i])); // Serpent.str:400
    }; // Serpent.str:399
    for (int i = 0; (i < N); i++) { // Serpent.str:402
      inputChannel.popInt(); // Serpent.str:403
    }; // Serpent.str:402
  }
  public void init(final int _param_N, final int[] _param_permutation) { // Serpent.str:396
    N = _param_N; // Serpent.str:396
    permutation = _param_permutation; // Serpent.str:396
    setIOTypes(Integer.TYPE, Integer.TYPE); // Serpent.str:396
    addSteadyPhase(N, N, N, "work"); // Serpent.str:398
  }
}
class Xor extends Filter // Serpent.str:410
{
  private int __param__param_n;
  public static Xor __construct(int _param_n)
  {
    Xor __obj = new Xor();
    __obj.__param__param_n = _param_n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_n);
  }
  int n; // Serpent.str:410
  public void work() { // Serpent.str:412
    int _bit_x = inputChannel.popInt(); // Serpent.str:413
    for (int i = 1; (i < n); i++) { // Serpent.str:414
      int _bit_y; // Serpent.str:415
      _bit_y = inputChannel.popInt(); // Serpent.str:415
      _bit_x = (_bit_x ^ _bit_y); // Serpent.str:416
    }; // Serpent.str:414
    outputChannel.pushInt(_bit_x); // Serpent.str:418
  }
  public void init(final int _param_n) { // Serpent.str:410
    n = _param_n; // Serpent.str:410
    setIOTypes(Integer.TYPE, Integer.TYPE); // Serpent.str:410
    addSteadyPhase(n, n, 1, "work"); // Serpent.str:412
  }
}
class IntoBits extends Filter // Serpent.str:424
{
  public static IntoBits __construct()
  {
    IntoBits __obj = new IntoBits();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // Serpent.str:426
    int v = inputChannel.popInt(); // Serpent.str:427
    int m = 1; // Serpent.str:428
    for (int i = 0; (i < 32); i++) { // Serpent.str:430
      if ((((v & m) >> i) != 0)) { // Serpent.str:432
        outputChannel.pushInt(1); // Serpent.str:432
      } else { // Serpent.str:434
        outputChannel.pushInt(0); // Serpent.str:434
      } // Serpent.str:431
      m = (m << 1); // Serpent.str:435
    }; // Serpent.str:430
  }
  public void init() { // Serpent.str:424
    setIOTypes(Integer.TYPE, Integer.TYPE); // Serpent.str:424
    addSteadyPhase(1, 1, 32, "work"); // Serpent.str:426
  }
}
class BitstoInts extends Filter // Serpent.str:442
{
  private int __param__param_n;
  public static BitstoInts __construct(int _param_n)
  {
    BitstoInts __obj = new BitstoInts();
    __obj.__param__param_n = _param_n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_n);
  }
  int n; // Serpent.str:442
  public void work() { // Serpent.str:444
    int v = 0; // Serpent.str:445
    for (int i = 0; (i < n); i++) { // Serpent.str:446
      v = (v | (inputChannel.popInt() << i)); // Serpent.str:447
    }; // Serpent.str:446
    outputChannel.pushInt(v); // Serpent.str:449
  }
  public void init(final int _param_n) { // Serpent.str:442
    n = _param_n; // Serpent.str:442
    setIOTypes(Integer.TYPE, Integer.TYPE); // Serpent.str:442
    addSteadyPhase(n, n, 1, "work"); // Serpent.str:444
  }
}
class BitSlice extends SplitJoin // Serpent.str:455
{
  private int __param_w;
  private int __param_b;
  public static BitSlice __construct(int w, int b)
  {
    BitSlice __obj = new BitSlice();
    __obj.__param_w = w;
    __obj.__param_b = b;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_w, __param_b);
  }
  public void init(final int w, final int b) { // Serpent.str:456
    setSplitter(ROUND_ROBIN(1)); // Serpent.str:457
    for (int l = 0; (l < b); l++) { // Serpent.str:458
      add(new Identity(Integer.TYPE)); // Serpent.str:459
    }; // Serpent.str:458
    setJoiner(ROUND_ROBIN(w)); // Serpent.str:461
  }
}
class HexPrinter extends Pipeline // Serpent.str:467
{
  private int __param_descriptor;
  private int __param_n;
  public static HexPrinter __construct(int descriptor, int n)
  {
    HexPrinter __obj = new HexPrinter();
    __obj.__param_descriptor = descriptor;
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_descriptor, __param_n);
  }
  public void init(final int descriptor, final int n) { // Serpent.str:468
    int bits = n; // Serpent.str:469
    int bytes = (bits / 4); // Serpent.str:470
    add(BitstoInts.__construct(4)); // Serpent.str:472
    add(AnonFilter_a4.__construct(TheGlobal.__get_instance().CIPHERTEXT, TheGlobal.__get_instance().PLAINTEXT, TheGlobal.__get_instance().PRINTINFO, TheGlobal.__get_instance().USERKEY, bytes, descriptor)); // Serpent.str:473
  }
}
class ShowIntermediate extends SplitJoin // Serpent.str:518
{
  private int __param_n;
  public static ShowIntermediate __construct(int n)
  {
    ShowIntermediate __obj = new ShowIntermediate();
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_n);
  }
  public void init(final int n) { // Serpent.str:519
    int bits = n; // Serpent.str:520
    int bytes = (bits / 4); // Serpent.str:521
    setSplitter(DUPLICATE()); // Serpent.str:523
    add(new Identity(Integer.TYPE)); // Serpent.str:524
    add(AnonFilter_a6.__construct(bytes)); // Serpent.str:525
    setJoiner(WEIGHTED_ROUND_ROBIN(1, 0)); // Serpent.str:557
  }
}
class TheGlobal extends Global // Serpent.str:564
{
  private static TheGlobal __instance = null;
  private TheGlobal() {}
  public static TheGlobal __get_instance() {
    if (__instance == null) { __instance = new TheGlobal(); __instance.init(); }
    return __instance;
  }
  public int BITS_PER_WORD; // Serpent.str:565
  public int NBITS; // Serpent.str:568
  public int PHI; // Serpent.str:571
  public int MAXROUNDS; // Serpent.str:574
  public boolean PRINTINFO; // Serpent.str:577
  public int PLAINTEXT; // Serpent.str:578
  public int USERKEY; // Serpent.str:579
  public int CIPHERTEXT; // Serpent.str:580
  public int[][] USERKEYS = {{0,0,0,0,0,0,0,0},
{0,0,0,0,0,0,0,0},
{-1829788726,-1804109491,838131579,-836508150,1614336722,1896051696,1339230894,-827807165},
{-738420253,755581455,10502647,-483847052,1999748685,1314610597,415411168,-1591500888},
{-1122733020,1623633375,-954274029,685956534,-1168406632,-1150893116,-746541904,1439352169}
}; // Serpent.str:583
  public int USERKEY_LENGTH; // Serpent.str:594
  public int[] IP = {0,32,64,96,1,33,65,97,2,34,66,98,3,35,67,99,4,36,68,100,5,37,69,101,6,38,70,102,7,39,71,103,8,40,72,104,9,41,73,105,10,42,74,106,11,43,75,107,12,44,76,108,13,45,77,109,14,46,78,110,15,47,79,111,16,48,80,112,17,49,81,113,18,50,82,114,19,51,83,115,20,52,84,116,21,53,85,117,22,54,86,118,23,55,87,119,24,56,88,120,25,57,89,121,26,58,90,122,27,59,91,123,28,60,92,124,29,61,93,125,30,62,94,126,31,63,95,127}; // Serpent.str:597
  public int[] FP = {0,4,8,12,16,20,24,28,32,36,40,44,48,52,56,60,64,68,72,76,80,84,88,92,96,100,104,108,112,116,120,124,1,5,9,13,17,21,25,29,33,37,41,45,49,53,57,61,65,69,73,77,81,85,89,93,97,101,105,109,113,117,121,125,2,6,10,14,18,22,26,30,34,38,42,46,50,54,58,62,66,70,74,78,82,86,90,94,98,102,106,110,114,118,122,126,3,7,11,15,19,23,27,31,35,39,43,47,51,55,59,63,67,71,75,79,83,87,91,95,99,103,107,111,115,119,123,127}; // Serpent.str:607
  public int[][] SBOXES = {{3,8,15,1,10,6,5,11,14,13,4,2,7,0,9,12},
{15,12,2,7,9,0,5,10,1,11,14,8,6,13,3,4},
{8,6,7,9,3,12,10,15,13,1,14,4,0,11,5,2},
{0,15,11,8,12,9,6,3,13,1,2,4,10,7,5,14},
{1,15,8,3,12,0,11,6,2,5,4,10,9,14,7,13},
{15,5,2,11,4,10,9,12,0,3,14,8,13,6,7,1},
{7,2,12,5,8,4,6,11,14,9,1,15,13,3,10,0},
{1,13,15,0,14,8,2,11,7,4,12,10,9,3,5,6}
}; // Serpent.str:617
  public void init() { // Serpent.str:626
    BITS_PER_WORD = 32; // Serpent.str:565
    NBITS = 128; // Serpent.str:568
    PHI = -1640531527; // Serpent.str:571
    MAXROUNDS = 32; // Serpent.str:574
    PRINTINFO = false; // Serpent.str:577
    PLAINTEXT = 0; // Serpent.str:578
    USERKEY = 1; // Serpent.str:579
    CIPHERTEXT = 2; // Serpent.str:580
    USERKEY_LENGTH = (8 * BITS_PER_WORD); // Serpent.str:594
  }
}
class AnonFilter_a0 extends SplitJoin // Serpent.str:60
{
  private int __param_NBITS;
  private int __param_round;
  private int __param_vector;
  public static AnonFilter_a0 __construct(int NBITS, int round, int vector)
  {
    AnonFilter_a0 __obj = new AnonFilter_a0();
    __obj.__param_NBITS = NBITS;
    __obj.__param_round = round;
    __obj.__param_vector = vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_NBITS, __param_round, __param_vector);
  }
  public void init(final int NBITS, final int round, final int vector) { // Serpent.str:60
    setSplitter(WEIGHTED_ROUND_ROBIN(NBITS, 1)); // Serpent.str:61
    add(new Identity(Integer.TYPE)); // Serpent.str:62
    add(KeySchedule.__construct(vector, round)); // Serpent.str:63
    setJoiner(ROUND_ROBIN(1)); // Serpent.str:64
  }
}
class AnonFilter_a1 extends SplitJoin // Serpent.str:75
{
  private int __param_MAXROUNDS;
  private int __param_NBITS;
  private int __param_vector;
  public static AnonFilter_a1 __construct(int MAXROUNDS, int NBITS, int vector)
  {
    AnonFilter_a1 __obj = new AnonFilter_a1();
    __obj.__param_MAXROUNDS = MAXROUNDS;
    __obj.__param_NBITS = NBITS;
    __obj.__param_vector = vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_MAXROUNDS, __param_NBITS, __param_vector);
  }
  public void init(final int MAXROUNDS, final int NBITS, final int vector) { // Serpent.str:75
    setSplitter(WEIGHTED_ROUND_ROBIN(NBITS, 1)); // Serpent.str:76
    add(new Identity(Integer.TYPE)); // Serpent.str:77
    add(KeySchedule.__construct(vector, MAXROUNDS)); // Serpent.str:78
    setJoiner(ROUND_ROBIN(1)); // Serpent.str:79
  }
}
class AnonFilter_a2 extends SplitJoin // Serpent.str:113
{
  private int __param_PLAINTEXT;
  public static AnonFilter_a2 __construct(int PLAINTEXT)
  {
    AnonFilter_a2 __obj = new AnonFilter_a2();
    __obj.__param_PLAINTEXT = PLAINTEXT;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_PLAINTEXT);
  }
  public void init(final int PLAINTEXT) { // Serpent.str:113
    setSplitter(DUPLICATE()); // Serpent.str:114
    add(new Identity(Integer.TYPE)); // Serpent.str:115
    add(HexPrinter.__construct(PLAINTEXT, 128)); // Serpent.str:116
    setJoiner(WEIGHTED_ROUND_ROBIN(1, 0)); // Serpent.str:117
  }
}
class AnonFilter_a3 extends Filter // Serpent.str:139
{
  private int __param__param_BITS_PER_WORD;
  private int __param__param__len_d0_IP;
  private int[] __param__param_IP;
  private int __param__param_MAXROUNDS;
  private int __param__param_NBITS;
  private int __param__param_PHI;
  private int __param__param__len_d0_SBOXES;
  private int __param__param__len_d1_SBOXES;
  private int[][] __param__param_SBOXES;
  private int __param__param__len_d0_USERKEYS;
  private int __param__param__len_d1_USERKEYS;
  private int[][] __param__param_USERKEYS;
  private int __param__param_USERKEY_LENGTH;
  private int __param__param_round;
  private int __param__param_vector;
  public static AnonFilter_a3 __construct(int _param_BITS_PER_WORD, int _param__len_d0_IP, int[] _param_IP, int _param_MAXROUNDS, int _param_NBITS, int _param_PHI, int _param__len_d0_SBOXES, int _param__len_d1_SBOXES, int[][] _param_SBOXES, int _param__len_d0_USERKEYS, int _param__len_d1_USERKEYS, int[][] _param_USERKEYS, int _param_USERKEY_LENGTH, int _param_round, int _param_vector)
  {
    AnonFilter_a3 __obj = new AnonFilter_a3();
    __obj.__param__param_BITS_PER_WORD = _param_BITS_PER_WORD;
    __obj.__param__param__len_d0_IP = _param__len_d0_IP;
    __obj.__param__param_IP = _param_IP;
    __obj.__param__param_MAXROUNDS = _param_MAXROUNDS;
    __obj.__param__param_NBITS = _param_NBITS;
    __obj.__param__param_PHI = _param_PHI;
    __obj.__param__param__len_d0_SBOXES = _param__len_d0_SBOXES;
    __obj.__param__param__len_d1_SBOXES = _param__len_d1_SBOXES;
    __obj.__param__param_SBOXES = _param_SBOXES;
    __obj.__param__param__len_d0_USERKEYS = _param__len_d0_USERKEYS;
    __obj.__param__param__len_d1_USERKEYS = _param__len_d1_USERKEYS;
    __obj.__param__param_USERKEYS = _param_USERKEYS;
    __obj.__param__param_USERKEY_LENGTH = _param_USERKEY_LENGTH;
    __obj.__param__param_round = _param_round;
    __obj.__param__param_vector = _param_vector;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_BITS_PER_WORD, __param__param__len_d0_IP, __param__param_IP, __param__param_MAXROUNDS, __param__param_NBITS, __param__param_PHI, __param__param__len_d0_SBOXES, __param__param__len_d1_SBOXES, __param__param_SBOXES, __param__param__len_d0_USERKEYS, __param__param__len_d1_USERKEYS, __param__param_USERKEYS, __param__param_USERKEY_LENGTH, __param__param_round, __param__param_vector);
  }
  int[][] keys; // Serpent.str:140
  int BITS_PER_WORD; // Serpent.str:139
  int _len_d0_IP; // Serpent.str:139
  int[] IP; // Serpent.str:139
  int MAXROUNDS; // Serpent.str:139
  int NBITS; // Serpent.str:139
  int PHI; // Serpent.str:139
  int _len_d0_SBOXES; // Serpent.str:139
  int _len_d1_SBOXES; // Serpent.str:139
  int[][] SBOXES; // Serpent.str:139
  int _len_d0_USERKEYS; // Serpent.str:139
  int _len_d1_USERKEYS; // Serpent.str:139
  int[][] USERKEYS; // Serpent.str:139
  int USERKEY_LENGTH; // Serpent.str:139
  int round; // Serpent.str:139
  int vector; // Serpent.str:139
  public void work() { // Serpent.str:214
    inputChannel.popInt(); // Serpent.str:215
    for (int i = 0; (i < NBITS); i++) { // Serpent.str:216
      outputChannel.pushInt(keys[round][i]); // Serpent.str:217
    }; // Serpent.str:216
  }
  public int LRotate(int x, int n) { // Serpent.str:221
    int[] v; // Serpent.str:224
    int[] __sa8 = {32}; // Serpent.str:224
    v = ((int[])(ArrayMemoizer.initArray(new int[0], __sa8))); // Serpent.str:224
    int m = 1; // Serpent.str:225
    for (int i = 0; (i < 32); i++) { // Serpent.str:226
      if ((((x & m) >> i) != 0)) { // Serpent.str:228
        v[i] = 1; // Serpent.str:228
      } // Serpent.str:227
      m = (m << 1); // Serpent.str:229
    }; // Serpent.str:226
    int[] w = new int[32]; // Serpent.str:232
    for (int i = 0; (i < 32); i++) { // Serpent.str:233
      w[i] = v[(((i + 32) - 11) % 32)]; // Serpent.str:234
    }; // Serpent.str:233
    int r = 0; // Serpent.str:237
    for (int i = 0; (i < 32); i++) { // Serpent.str:238
      r = (r | (w[i] << i)); // Serpent.str:239
    }; // Serpent.str:238
    return r; // Serpent.str:242
  }
  public void init(final int _param_BITS_PER_WORD, final int _param__len_d0_IP, final int[] _param_IP, final int _param_MAXROUNDS, final int _param_NBITS, final int _param_PHI, final int _param__len_d0_SBOXES, final int _param__len_d1_SBOXES, final int[][] _param_SBOXES, final int _param__len_d0_USERKEYS, final int _param__len_d1_USERKEYS, final int[][] _param_USERKEYS, final int _param_USERKEY_LENGTH, final int _param_round, final int _param_vector) { // Serpent.str:143
    BITS_PER_WORD = _param_BITS_PER_WORD; // Serpent.str:143
    _len_d0_IP = _param__len_d0_IP; // Serpent.str:143
    IP = _param_IP; // Serpent.str:143
    MAXROUNDS = _param_MAXROUNDS; // Serpent.str:143
    NBITS = _param_NBITS; // Serpent.str:143
    PHI = _param_PHI; // Serpent.str:143
    _len_d0_SBOXES = _param__len_d0_SBOXES; // Serpent.str:143
    _len_d1_SBOXES = _param__len_d1_SBOXES; // Serpent.str:143
    SBOXES = _param_SBOXES; // Serpent.str:143
    _len_d0_USERKEYS = _param__len_d0_USERKEYS; // Serpent.str:143
    _len_d1_USERKEYS = _param__len_d1_USERKEYS; // Serpent.str:143
    USERKEYS = _param_USERKEYS; // Serpent.str:143
    USERKEY_LENGTH = _param_USERKEY_LENGTH; // Serpent.str:143
    round = _param_round; // Serpent.str:143
    vector = _param_vector; // Serpent.str:143
    keys = new int[(MAXROUNDS + 1)][NBITS]; // Serpent.str:140
    for (int __sa9 = 0; (__sa9 < (MAXROUNDS + 1)); __sa9++) { // Serpent.str:140
    }; // Serpent.str:140
    setIOTypes(Integer.TYPE, Integer.TYPE); // Serpent.str:139
    addSteadyPhase(1, 1, NBITS, "work"); // Serpent.str:214
    int[] userkey = {0,0,0,0,0,0,0,0}; // Serpent.str:144
    int[] w = new int[140]; // Serpent.str:145
    int words = (USERKEY_LENGTH / BITS_PER_WORD); // Serpent.str:147
    for (int i = (words - 1); (i >= 0); i--) { // Serpent.str:149
      userkey[((words - 1) - i)] = USERKEYS[vector][i]; // Serpent.str:149
    }; // Serpent.str:148
    if ((USERKEY_LENGTH < 256)) { // Serpent.str:152
      int msb; // Serpent.str:153
      msb = userkey[(USERKEY_LENGTH / BITS_PER_WORD)]; // Serpent.str:153
      userkey[(USERKEY_LENGTH / BITS_PER_WORD)] = (msb | (1 << (USERKEY_LENGTH % BITS_PER_WORD))); // Serpent.str:154
    } // Serpent.str:152
    for (int i = 0; (i < 8); i++) { // Serpent.str:159
      w[i] = userkey[i]; // Serpent.str:159
    }; // Serpent.str:158
    for (int i = 8; (i < 140); i++) { // Serpent.str:162
      w[i] = (((((w[(i - 8)] ^ w[(i - 5)]) ^ w[(i - 3)]) ^ w[(i - 1)]) ^ PHI) ^ (i - 8)); // Serpent.str:163
      w[i] = LRotate(w[i], 11); // Serpent.str:164
    }; // Serpent.str:162
    for (int i = 0; (i <= MAXROUNDS); i++) { // Serpent.str:168
      int[] sbox; // Serpent.str:169
      sbox = new int[BITS_PER_WORD]; // Serpent.str:169
      for (int b = 0; (b < BITS_PER_WORD); b++) { // Serpent.str:170
        int r; // Serpent.str:172
        r = ((4 * i) + 8); // Serpent.str:172
        int _bit_b0; // Serpent.str:173
        _bit_b0 = ((w[(r + 0)] & (1 << b)) >> b); // Serpent.str:173
        int _bit_b1; // Serpent.str:174
        _bit_b1 = ((w[(r + 1)] & (1 << b)) >> b); // Serpent.str:174
        int _bit_b2; // Serpent.str:175
        _bit_b2 = ((w[(r + 2)] & (1 << b)) >> b); // Serpent.str:175
        int _bit_b3; // Serpent.str:176
        _bit_b3 = ((w[(r + 3)] & (1 << b)) >> b); // Serpent.str:176
        int val; // Serpent.str:178
        val = 0; // Serpent.str:178
        if ((_bit_b0 != 0)) { // Serpent.str:179
          val = 1; // Serpent.str:179
        } // Serpent.str:179
        if ((_bit_b1 != 0)) { // Serpent.str:180
          val = (val | (1 << 1)); // Serpent.str:180
        } // Serpent.str:180
        if ((_bit_b2 != 0)) { // Serpent.str:181
          val = (val | (1 << 2)); // Serpent.str:181
        } // Serpent.str:181
        if ((_bit_b3 != 0)) { // Serpent.str:182
          val = (val | (1 << 3)); // Serpent.str:182
        } // Serpent.str:182
        sbox[b] = SBOXES[(((32 + 3) - i) % 8)][val]; // Serpent.str:192
      }; // Serpent.str:170
      int[] key; // Serpent.str:196
      key = new int[NBITS]; // Serpent.str:196
      for (int k = 0; (k < (NBITS / BITS_PER_WORD)); k++) { // Serpent.str:197
        for (int b = 0; (b < BITS_PER_WORD); b++) { // Serpent.str:198
          int _bit_x; // Serpent.str:199
          _bit_x = ((sbox[b] & (1 << k)) >> k); // Serpent.str:199
          if ((_bit_x != 0)) { // Serpent.str:201
            key[((k * BITS_PER_WORD) + b)] = 1; // Serpent.str:201
          } else { // Serpent.str:203
            key[((k * BITS_PER_WORD) + b)] = 0; // Serpent.str:203
          } // Serpent.str:200
        }; // Serpent.str:198
      }; // Serpent.str:197
      for (int b = 0; (b < NBITS); b++) { // Serpent.str:208
        keys[i][b] = key[IP[b]]; // Serpent.str:209
      }; // Serpent.str:208
    }; // Serpent.str:168
  }
}
class AnonFilter_a4 extends Filter // Serpent.str:473
{
  private int __param__param_CIPHERTEXT;
  private int __param__param_PLAINTEXT;
  private boolean __param__param_PRINTINFO;
  private int __param__param_USERKEY;
  private int __param__param_bytes;
  private int __param__param_descriptor;
  public static AnonFilter_a4 __construct(int _param_CIPHERTEXT, int _param_PLAINTEXT, boolean _param_PRINTINFO, int _param_USERKEY, int _param_bytes, int _param_descriptor)
  {
    AnonFilter_a4 __obj = new AnonFilter_a4();
    __obj.__param__param_CIPHERTEXT = _param_CIPHERTEXT;
    __obj.__param__param_PLAINTEXT = _param_PLAINTEXT;
    __obj.__param__param_PRINTINFO = _param_PRINTINFO;
    __obj.__param__param_USERKEY = _param_USERKEY;
    __obj.__param__param_bytes = _param_bytes;
    __obj.__param__param_descriptor = _param_descriptor;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_CIPHERTEXT, __param__param_PLAINTEXT, __param__param_PRINTINFO, __param__param_USERKEY, __param__param_bytes, __param__param_descriptor);
  }
  int CIPHERTEXT; // Serpent.str:473
  int PLAINTEXT; // Serpent.str:473
  boolean PRINTINFO; // Serpent.str:473
  int USERKEY; // Serpent.str:473
  int bytes; // Serpent.str:473
  int descriptor; // Serpent.str:473
  public void work() { // Serpent.str:474
    if (PRINTINFO) { // Serpent.str:475
      if ((descriptor == PLAINTEXT)) { // Serpent.str:476
        System.out.print("P: "); // Serpent.str:477
      } else { // Serpent.str:478
        if ((descriptor == USERKEY)) { // Serpent.str:478
          System.out.print("K: "); // Serpent.str:479
        } else { // Serpent.str:480
          if ((descriptor == CIPHERTEXT)) { // Serpent.str:480
            System.out.print("C: "); // Serpent.str:481
          } // Serpent.str:480
        } // Serpent.str:478
      } // Serpent.str:476
    } // Serpent.str:475
    for (int i = (bytes - 1); (i >= 0); i--) { // Serpent.str:485
      int v; // Serpent.str:486
      v = inputChannel.peekInt(i); // Serpent.str:486
      if ((v < 10)) { // Serpent.str:488
        System.out.print(v); // Serpent.str:489
      } else { // Serpent.str:490
        if ((v == 10)) { // Serpent.str:490
          System.out.print("A"); // Serpent.str:491
        } else { // Serpent.str:492
          if ((v == 11)) { // Serpent.str:492
            System.out.print("B"); // Serpent.str:493
          } else { // Serpent.str:494
            if ((v == 12)) { // Serpent.str:494
              System.out.print("C"); // Serpent.str:495
            } else { // Serpent.str:496
              if ((v == 13)) { // Serpent.str:496
                System.out.print("D"); // Serpent.str:497
              } else { // Serpent.str:498
                if ((v == 14)) { // Serpent.str:498
                  System.out.print("E"); // Serpent.str:499
                } else { // Serpent.str:500
                  if ((v == 15)) { // Serpent.str:500
                    System.out.print("F"); // Serpent.str:501
                  } else { // Serpent.str:502
                    System.out.print("ERROR: "); // Serpent.str:503
                    System.out.println(v); // Serpent.str:504
                  } // Serpent.str:500
                } // Serpent.str:498
              } // Serpent.str:496
            } // Serpent.str:494
          } // Serpent.str:492
        } // Serpent.str:490
      } // Serpent.str:488
    }; // Serpent.str:485
    System.out.println(""); // Serpent.str:507
    for (int i = 0; (i < bytes); i++) { // Serpent.str:510
      inputChannel.popInt(); // Serpent.str:510
    }; // Serpent.str:509
  }
  public void init(final int _param_CIPHERTEXT, final int _param_PLAINTEXT, final boolean _param_PRINTINFO, final int _param_USERKEY, final int _param_bytes, final int _param_descriptor) { // Serpent.str:473
    CIPHERTEXT = _param_CIPHERTEXT; // Serpent.str:473
    PLAINTEXT = _param_PLAINTEXT; // Serpent.str:473
    PRINTINFO = _param_PRINTINFO; // Serpent.str:473
    USERKEY = _param_USERKEY; // Serpent.str:473
    bytes = _param_bytes; // Serpent.str:473
    descriptor = _param_descriptor; // Serpent.str:473
    setIOTypes(Integer.TYPE, Void.TYPE); // Serpent.str:473
    addSteadyPhase(bytes, bytes, 0, "work"); // Serpent.str:474
  }
}
class AnonFilter_a5 extends Filter // Serpent.str:527
{
  private int __param__param_bytes;
  public static AnonFilter_a5 __construct(int _param_bytes)
  {
    AnonFilter_a5 __obj = new AnonFilter_a5();
    __obj.__param__param_bytes = _param_bytes;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_bytes);
  }
  int bytes; // Serpent.str:527
  public void work() { // Serpent.str:528
    for (int i = (bytes - 1); (i >= 0); i--) { // Serpent.str:529
      int v; // Serpent.str:530
      v = inputChannel.peekInt(i); // Serpent.str:530
      if ((v < 10)) { // Serpent.str:531
        System.out.print(v); // Serpent.str:532
      } else { // Serpent.str:533
        if ((v == 10)) { // Serpent.str:533
          System.out.print("A"); // Serpent.str:534
        } else { // Serpent.str:535
          if ((v == 11)) { // Serpent.str:535
            System.out.print("B"); // Serpent.str:536
          } else { // Serpent.str:537
            if ((v == 12)) { // Serpent.str:537
              System.out.print("C"); // Serpent.str:538
            } else { // Serpent.str:539
              if ((v == 13)) { // Serpent.str:539
                System.out.print("D"); // Serpent.str:540
              } else { // Serpent.str:541
                if ((v == 14)) { // Serpent.str:541
                  System.out.print("E"); // Serpent.str:542
                } else { // Serpent.str:543
                  if ((v == 15)) { // Serpent.str:543
                    System.out.print("F"); // Serpent.str:544
                  } else { // Serpent.str:545
                    System.out.print("ERROR: "); // Serpent.str:546
                    System.out.println(v); // Serpent.str:547
                  } // Serpent.str:543
                } // Serpent.str:541
              } // Serpent.str:539
            } // Serpent.str:537
          } // Serpent.str:535
        } // Serpent.str:533
      } // Serpent.str:531
    }; // Serpent.str:529
    System.out.println(""); // Serpent.str:550
    for (int i = 0; (i < bytes); i++) { // Serpent.str:553
      inputChannel.popInt(); // Serpent.str:553
    }; // Serpent.str:552
  }
  public void init(final int _param_bytes) { // Serpent.str:527
    bytes = _param_bytes; // Serpent.str:527
    setIOTypes(Integer.TYPE, Void.TYPE); // Serpent.str:527
    addSteadyPhase(bytes, bytes, 0, "work"); // Serpent.str:528
  }
}
class AnonFilter_a6 extends Pipeline // Serpent.str:525
{
  private int __param_bytes;
  public static AnonFilter_a6 __construct(int bytes)
  {
    AnonFilter_a6 __obj = new AnonFilter_a6();
    __obj.__param_bytes = bytes;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_bytes);
  }
  public void init(final int bytes) { // Serpent.str:525
    add(BitstoInts.__construct(4)); // Serpent.str:526
    add(AnonFilter_a5.__construct(bytes)); // Serpent.str:527
  }
}
public class Serpent extends StreamItPipeline // Serpent.str:5
{
  public void init() { // Serpent.str:6
    int testvector = 2; // Serpent.str:7
    add(PlainTextSource.__construct(testvector)); // Serpent.str:9
    add(SerpentEncoder.__construct(testvector)); // Serpent.str:10
    add(HexPrinter.__construct(TheGlobal.__get_instance().CIPHERTEXT, TheGlobal.__get_instance().NBITS)); // Serpent.str:11
  }
  public static void main(String[] args) {
    Serpent program = new Serpent();
    program.run(args);
    FileWriter.closeAll();
  }
}
