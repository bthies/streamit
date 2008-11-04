
#include <stdint.h>
#include <stdio.h>

#ifndef _FIXED_H
#define _FIXED_H

class fixed {
private:
  int32_t m_nVal;
public:
  fixed(void);
  fixed(int nVal);
  fixed(float nVal);
  fixed(const fixed& fixedVal);
  fixed(const fixed* fixedVal);
  ~fixed(void);

  fixed& operator=(float floatVal);
  fixed& operator=(fixed fixedVal);
  fixed& operator=(int intVal);

  bool equals(fixed b);
  bool operator==(float floatVal);
  bool operator==(fixed fixedVal);
  bool operator==(int intVal);

  bool operator!=(float floatVal);
  bool operator!=(fixed fixedVal);
  bool operator!=(int intVal);

  bool lessthan(fixed b);
  bool operator<(float floatVal);
  bool operator<(fixed fixedVal);
  bool operator<(int intVal);

  bool lessthanequal(fixed b);
  bool operator<=(float floatVal);
  bool operator<=(fixed fixedVal);
  bool operator<=(int intVal);

  bool operator>(float floatVal);
  bool operator>(fixed fixedVal);
  bool operator>(int intVal);

  bool operator>=(float floatVal);
  bool operator>=(fixed fixedVal);
  bool operator>=(int intVal);

  operator float(void);
  operator int(void);

  fixed floor(void);
  fixed ceil(void);

  fixed add(fixed b);
  fixed subtract(fixed b);
  fixed multiply(fixed b);
  fixed divide(fixed b);
  fixed operator+(fixed b);
  fixed operator-(fixed b);
  fixed operator*(fixed b);
  fixed operator/(fixed b);

  fixed add(float b);
  fixed subtract(float b);
  fixed multiply(float b);
  fixed divide(float b);
  fixed operator+(float b);
  fixed operator-(float b);
  fixed operator*(float b);
  fixed operator/(float b);

  fixed add(int b);
  fixed subtract(int b);
  fixed multiply(int b);
  fixed divide(int b);
  fixed operator+(int b);
  fixed operator-(int b);
  fixed operator*(int b);
  fixed operator/(int b);

  fixed sin(void);
  fixed cos(void);
  fixed tan(void);
  fixed asin(void);
  fixed acos(void);
  fixed atan(void);
};

/*
  fixed sqrt(void);
  fixed pow(fixed fixedPower);
  fixed log10(void);
  fixed log(void);
  fixed exp(void);
*/
  
fixed operator-(float a, fixed b);
fixed operator-(int a, fixed b);
bool operator<(float b, fixed a);
bool operator<(int b, fixed a);
fixed operator-(fixed a);

float sin(fixed a);
float cos(fixed a);
float tan(fixed a);
float asin(fixed a);
float acos(fixed a);
float atan(fixed a);

/*
  fixed absx( fixed p_Base );
  fixed floorx(fixed fixedVal);
  fixed ceilx(fixed fixedVal);
  fixed sqrtx(fixed fixedVal);
  fixed powx(fixed fixedVal, fixed fixedPower);
  fixed log10x(fixed fixedVal);
  fixed logx(fixed fixedVal);
  fixed expx(fixed fixedVal);
  fixed sinx(fixed x);
  fixed cosx(fixed x);
  fixed tanx(fixed x);
*/

/* precalculated fixed point (16.16) cosines for a full circle (0-255) */
int32_t cos_tbl[512] =
{
   65536,  65531,  65516,  65492,  65457,  65413,  65358,  65294,
   65220,  65137,  65043,  64940,  64827,  64704,  64571,  64429,
   64277,  64115,  63944,  63763,  63572,  63372,  63162,  62943,
   62714,  62476,  62228,  61971,  61705,  61429,  61145,  60851,
   60547,  60235,  59914,  59583,  59244,  58896,  58538,  58172,
   57798,  57414,  57022,  56621,  56212,  55794,  55368,  54934,
   54491,  54040,  53581,  53114,  52639,  52156,  51665,  51166,
   50660,  50146,  49624,  49095,  48559,  48015,  47464,  46906,
   46341,  45769,  45190,  44604,  44011,  43412,  42806,  42194,
   41576,  40951,  40320,  39683,  39040,  38391,  37736,  37076,
   36410,  35738,  35062,  34380,  33692,  33000,  32303,  31600,
   30893,  30182,  29466,  28745,  28020,  27291,  26558,  25821,
   25080,  24335,  23586,  22834,  22078,  21320,  20557,  19792,
   19024,  18253,  17479,  16703,  15924,  15143,  14359,  13573,
   12785,  11996,  11204,  10411,  9616,   8820,   8022,   7224,
   6424,   5623,   4821,   4019,   3216,   2412,   1608,   804,
   0,      -804,   -1608,  -2412,  -3216,  -4019,  -4821,  -5623,
   -6424,  -7224,  -8022,  -8820,  -9616,  -10411, -11204, -11996,
   -12785, -13573, -14359, -15143, -15924, -16703, -17479, -18253,
   -19024, -19792, -20557, -21320, -22078, -22834, -23586, -24335,
   -25080, -25821, -26558, -27291, -28020, -28745, -29466, -30182,
   -30893, -31600, -32303, -33000, -33692, -34380, -35062, -35738,
   -36410, -37076, -37736, -38391, -39040, -39683, -40320, -40951,
   -41576, -42194, -42806, -43412, -44011, -44604, -45190, -45769,
   -46341, -46906, -47464, -48015, -48559, -49095, -49624, -50146,
   -50660, -51166, -51665, -52156, -52639, -53114, -53581, -54040,
   -54491, -54934, -55368, -55794, -56212, -56621, -57022, -57414,
   -57798, -58172, -58538, -58896, -59244, -59583, -59914, -60235,
   -60547, -60851, -61145, -61429, -61705, -61971, -62228, -62476,
   -62714, -62943, -63162, -63372, -63572, -63763, -63944, -64115,
   -64277, -64429, -64571, -64704, -64827, -64940, -65043, -65137,
   -65220, -65294, -65358, -65413, -65457, -65492, -65516, -65531,
   -65536, -65531, -65516, -65492, -65457, -65413, -65358, -65294,
   -65220, -65137, -65043, -64940, -64827, -64704, -64571, -64429,
   -64277, -64115, -63944, -63763, -63572, -63372, -63162, -62943,
   -62714, -62476, -62228, -61971, -61705, -61429, -61145, -60851,
   -60547, -60235, -59914, -59583, -59244, -58896, -58538, -58172,
   -57798, -57414, -57022, -56621, -56212, -55794, -55368, -54934,
   -54491, -54040, -53581, -53114, -52639, -52156, -51665, -51166,
   -50660, -50146, -49624, -49095, -48559, -48015, -47464, -46906,
   -46341, -45769, -45190, -44604, -44011, -43412, -42806, -42194,
   -41576, -40951, -40320, -39683, -39040, -38391, -37736, -37076,
   -36410, -35738, -35062, -34380, -33692, -33000, -32303, -31600,
   -30893, -30182, -29466, -28745, -28020, -27291, -26558, -25821,
   -25080, -24335, -23586, -22834, -22078, -21320, -20557, -19792,
   -19024, -18253, -17479, -16703, -15924, -15143, -14359, -13573,
   -12785, -11996, -11204, -10411, -9616,  -8820,  -8022,  -7224,
   -6424,  -5623,  -4821,  -4019,  -3216,  -2412,  -1608,  -804,
   0,      804,    1608,   2412,   3216,   4019,   4821,   5623,
   6424,   7224,   8022,   8820,   9616,   10411,  11204,  11996,
   12785,  13573,  14359,  15143,  15924,  16703,  17479,  18253,
   19024,  19792,  20557,  21320,  22078,  22834,  23586,  24335,
   25080,  25821,  26558,  27291,  28020,  28745,  29466,  30182,
   30893,  31600,  32303,  33000,  33692,  34380,  35062,  35738,
   36410,  37076,  37736,  38391,  39040,  39683,  40320,  40951,
   41576,  42194,  42806,  43412,  44011,  44604,  45190,  45769,
   46341,  46906,  47464,  48015,  48559,  49095,  49624,  50146,
   50660,  51166,  51665,  52156,  52639,  53114,  53581,  54040,
   54491,  54934,  55368,  55794,  56212,  56621,  57022,  57414,
   57798,  58172,  58538,  58896,  59244,  59583,  59914,  60235,
   60547,  60851,  61145,  61429,  61705,  61971,  62228,  62476,
   62714,  62943,  63162,  63372,  63572,  63763,  63944,  64115,
   64277,  64429,  64571,  64704,  64827,  64940,  65043,  65137,
   65220,  65294,  65358,  65413,  65457,  65492,  65516,  65531
};

/* precalculated fixed point (16.16) tangents for a half circle (0-127) */
int32_t tan_tbl[256] =
{
   0,      804,    1609,   2414,   3220,   4026,   4834,   5644,
   6455,   7268,   8083,   8901,   9721,   10545,  11372,  12202,
   13036,  13874,  14717,  15564,  16416,  17273,  18136,  19005,
   19880,  20762,  21650,  22546,  23449,  24360,  25280,  26208,
   27146,  28093,  29050,  30018,  30996,  31986,  32988,  34002,
   35030,  36071,  37126,  38196,  39281,  40382,  41500,  42636,
   43790,  44963,  46156,  47369,  48605,  49863,  51145,  52451,
   53784,  55144,  56532,  57950,  59398,  60880,  62395,  63947,
   65536,  67165,  68835,  70548,  72308,  74116,  75974,  77887,
   79856,  81885,  83977,  86135,  88365,  90670,  93054,  95523,
   98082,  100736, 103493, 106358, 109340, 112447, 115687, 119071,
   122609, 126314, 130198, 134276, 138564, 143081, 147847, 152884,
   158218, 163878, 169896, 176309, 183161, 190499, 198380, 206870,
   216043, 225990, 236817, 248648, 261634, 275959, 291845, 309568,
   329472, 351993, 377693, 407305, 441808, 482534, 531352, 590958,
   665398, 761030, 888450, 1066730,1334016,1779314,2669641,5340086,
   -2147483647,-5340086,-2669641,-1779314,-1334016,-1066730,-888450,-761030,
   -665398,-590958,-531352,-482534,-441808,-407305,-377693,-351993,
   -329472,-309568,-291845,-275959,-261634,-248648,-236817,-225990,
   -216043,-206870,-198380,-190499,-183161,-176309,-169896,-163878,
   -158218,-152884,-147847,-143081,-138564,-134276,-130198,-126314,
   -122609,-119071,-115687,-112447,-109340,-106358,-103493,-100736,
   -98082, -95523, -93054, -90670, -88365, -86135, -83977, -81885,
   -79856, -77887, -75974, -74116, -72308, -70548, -68835, -67165,
   -65536, -63947, -62395, -60880, -59398, -57950, -56532, -55144,
   -53784, -52451, -51145, -49863, -48605, -47369, -46156, -44963,
   -43790, -42636, -41500, -40382, -39281, -38196, -37126, -36071,
   -35030, -34002, -32988, -31986, -30996, -30018, -29050, -28093,
   -27146, -26208, -25280, -24360, -23449, -22546, -21650, -20762,
   -19880, -19005, -18136, -17273, -16416, -15564, -14717, -13874,
   -13036, -12202, -11372, -10545, -9721,  -8901,  -8083,  -7268,
   -6455,  -5644,  -4834,  -4026,  -3220,  -2414,  -1609,  -804
};

/* precalculated fixed point (16.16) inverse cosines (-1 to 1) */
int32_t acos_tbl[513] =
{
   0x800000,  0x7C65C7,  0x7AE75A,  0x79C19E,  0x78C9BE,  0x77EF25,  0x772953,  0x76733A,
   0x75C991,  0x752A10,  0x74930C,  0x740345,  0x7379C1,  0x72F5BA,  0x72768F,  0x71FBBC,
   0x7184D3,  0x711174,  0x70A152,  0x703426,  0x6FC9B5,  0x6F61C9,  0x6EFC36,  0x6E98D1,
   0x6E3777,  0x6DD805,  0x6D7A5E,  0x6D1E68,  0x6CC40B,  0x6C6B2F,  0x6C13C1,  0x6BBDAF,
   0x6B68E6,  0x6B1558,  0x6AC2F5,  0x6A71B1,  0x6A217E,  0x69D251,  0x698420,  0x6936DF,
   0x68EA85,  0x689F0A,  0x685465,  0x680A8D,  0x67C17D,  0x67792C,  0x673194,  0x66EAAF,
   0x66A476,  0x665EE5,  0x6619F5,  0x65D5A2,  0x6591E7,  0x654EBF,  0x650C26,  0x64CA18,
   0x648890,  0x64478C,  0x640706,  0x63C6FC,  0x63876B,  0x63484F,  0x6309A5,  0x62CB6A,
   0x628D9C,  0x625037,  0x621339,  0x61D69F,  0x619A68,  0x615E90,  0x612316,  0x60E7F7,
   0x60AD31,  0x6072C3,  0x6038A9,  0x5FFEE3,  0x5FC56E,  0x5F8C49,  0x5F5372,  0x5F1AE7,
   0x5EE2A7,  0x5EAAB0,  0x5E7301,  0x5E3B98,  0x5E0473,  0x5DCD92,  0x5D96F3,  0x5D6095,
   0x5D2A76,  0x5CF496,  0x5CBEF2,  0x5C898B,  0x5C545E,  0x5C1F6B,  0x5BEAB0,  0x5BB62D,
   0x5B81E1,  0x5B4DCA,  0x5B19E7,  0x5AE638,  0x5AB2BC,  0x5A7F72,  0x5A4C59,  0x5A1970,
   0x59E6B6,  0x59B42A,  0x5981CC,  0x594F9B,  0x591D96,  0x58EBBD,  0x58BA0E,  0x588889,
   0x58572D,  0x5825FA,  0x57F4EE,  0x57C40A,  0x57934D,  0x5762B5,  0x573243,  0x5701F5,
   0x56D1CC,  0x56A1C6,  0x5671E4,  0x564224,  0x561285,  0x55E309,  0x55B3AD,  0x558471,
   0x555555,  0x552659,  0x54F77B,  0x54C8BC,  0x549A1B,  0x546B98,  0x543D31,  0x540EE7,
   0x53E0B9,  0x53B2A7,  0x5384B0,  0x5356D4,  0x532912,  0x52FB6B,  0x52CDDD,  0x52A068,
   0x52730C,  0x5245C9,  0x52189E,  0x51EB8B,  0x51BE8F,  0x5191AA,  0x5164DC,  0x513825,
   0x510B83,  0x50DEF7,  0x50B280,  0x50861F,  0x5059D2,  0x502D99,  0x500175,  0x4FD564,
   0x4FA967,  0x4F7D7D,  0x4F51A6,  0x4F25E2,  0x4EFA30,  0x4ECE90,  0x4EA301,  0x4E7784,
   0x4E4C19,  0x4E20BE,  0x4DF574,  0x4DCA3A,  0x4D9F10,  0x4D73F6,  0x4D48EC,  0x4D1DF1,
   0x4CF305,  0x4CC829,  0x4C9D5A,  0x4C729A,  0x4C47E9,  0x4C1D45,  0x4BF2AE,  0x4BC826,
   0x4B9DAA,  0x4B733B,  0x4B48D9,  0x4B1E84,  0x4AF43B,  0x4AC9FE,  0x4A9FCD,  0x4A75A7,
   0x4A4B8D,  0x4A217E,  0x49F77A,  0x49CD81,  0x49A393,  0x4979AF,  0x494FD5,  0x492605,
   0x48FC3F,  0x48D282,  0x48A8CF,  0x487F25,  0x485584,  0x482BEC,  0x48025D,  0x47D8D6,
   0x47AF57,  0x4785E0,  0x475C72,  0x47330A,  0x4709AB,  0x46E052,  0x46B701,  0x468DB7,
   0x466474,  0x463B37,  0x461201,  0x45E8D0,  0x45BFA6,  0x459682,  0x456D64,  0x45444B,
   0x451B37,  0x44F229,  0x44C920,  0x44A01C,  0x44771C,  0x444E21,  0x44252A,  0x43FC38,
   0x43D349,  0x43AA5F,  0x438178,  0x435894,  0x432FB4,  0x4306D8,  0x42DDFE,  0x42B527,
   0x428C53,  0x426381,  0x423AB2,  0x4211E5,  0x41E91A,  0x41C051,  0x41978A,  0x416EC5,
   0x414601,  0x411D3E,  0x40F47C,  0x40CBBB,  0x40A2FB,  0x407A3C,  0x40517D,  0x4028BE,
   0x400000,  0x3FD742,  0x3FAE83,  0x3F85C4,  0x3F5D05,  0x3F3445,  0x3F0B84,  0x3EE2C2,
   0x3EB9FF,  0x3E913B,  0x3E6876,  0x3E3FAF,  0x3E16E6,  0x3DEE1B,  0x3DC54E,  0x3D9C7F,
   0x3D73AD,  0x3D4AD9,  0x3D2202,  0x3CF928,  0x3CD04C,  0x3CA76C,  0x3C7E88,  0x3C55A1,
   0x3C2CB7,  0x3C03C8,  0x3BDAD6,  0x3BB1DF,  0x3B88E4,  0x3B5FE4,  0x3B36E0,  0x3B0DD7,
   0x3AE4C9,  0x3ABBB5,  0x3A929C,  0x3A697E,  0x3A405A,  0x3A1730,  0x39EDFF,  0x39C4C9,
   0x399B8C,  0x397249,  0x3948FF,  0x391FAE,  0x38F655,  0x38CCF6,  0x38A38E,  0x387A20,
   0x3850A9,  0x38272A,  0x37FDA3,  0x37D414,  0x37AA7C,  0x3780DB,  0x375731,  0x372D7E,
   0x3703C1,  0x36D9FB,  0x36B02B,  0x368651,  0x365C6D,  0x36327F,  0x360886,  0x35DE82,
   0x35B473,  0x358A59,  0x356033,  0x353602,  0x350BC5,  0x34E17C,  0x34B727,  0x348CC5,
   0x346256,  0x3437DA,  0x340D52,  0x33E2BB,  0x33B817,  0x338D66,  0x3362A6,  0x3337D7,
   0x330CFB,  0x32E20F,  0x32B714,  0x328C0A,  0x3260F0,  0x3235C6,  0x320A8C,  0x31DF42,
   0x31B3E7,  0x31887C,  0x315CFF,  0x313170,  0x3105D0,  0x30DA1E,  0x30AE5A,  0x308283,
   0x305699,  0x302A9C,  0x2FFE8B,  0x2FD267,  0x2FA62E,  0x2F79E1,  0x2F4D80,  0x2F2109,
   0x2EF47D,  0x2EC7DB,  0x2E9B24,  0x2E6E56,  0x2E4171,  0x2E1475,  0x2DE762,  0x2DBA37,
   0x2D8CF4,  0x2D5F98,  0x2D3223,  0x2D0495,  0x2CD6EE,  0x2CA92C,  0x2C7B50,  0x2C4D59,
   0x2C1F47,  0x2BF119,  0x2BC2CF,  0x2B9468,  0x2B65E5,  0x2B3744,  0x2B0885,  0x2AD9A7,
   0x2AAAAB,  0x2A7B8F,  0x2A4C53,  0x2A1CF7,  0x29ED7B,  0x29BDDC,  0x298E1C,  0x295E3A,
   0x292E34,  0x28FE0B,  0x28CDBD,  0x289D4B,  0x286CB3,  0x283BF6,  0x280B12,  0x27DA06,
   0x27A8D3,  0x277777,  0x2745F2,  0x271443,  0x26E26A,  0x26B065,  0x267E34,  0x264BD6,
   0x26194A,  0x25E690,  0x25B3A7,  0x25808E,  0x254D44,  0x2519C8,  0x24E619,  0x24B236,
   0x247E1F,  0x2449D3,  0x241550,  0x23E095,  0x23ABA2,  0x237675,  0x23410E,  0x230B6A,
   0x22D58A,  0x229F6B,  0x22690D,  0x22326E,  0x21FB8D,  0x21C468,  0x218CFF,  0x215550,
   0x211D59,  0x20E519,  0x20AC8E,  0x2073B7,  0x203A92,  0x20011D,  0x1FC757,  0x1F8D3D,
   0x1F52CF,  0x1F1809,  0x1EDCEA,  0x1EA170,  0x1E6598,  0x1E2961,  0x1DECC7,  0x1DAFC9,
   0x1D7264,  0x1D3496,  0x1CF65B,  0x1CB7B1,  0x1C7895,  0x1C3904,  0x1BF8FA,  0x1BB874,
   0x1B7770,  0x1B35E8,  0x1AF3DA,  0x1AB141,  0x1A6E19,  0x1A2A5E,  0x19E60B,  0x19A11B,
   0x195B8A,  0x191551,  0x18CE6C,  0x1886D4,  0x183E83,  0x17F573,  0x17AB9B,  0x1760F6,
   0x17157B,  0x16C921,  0x167BE0,  0x162DAF,  0x15DE82,  0x158E4F,  0x153D0B,  0x14EAA8,
   0x14971A,  0x144251,  0x13EC3F,  0x1394D1,  0x133BF5,  0x12E198,  0x1285A2,  0x1227FB,
   0x11C889,  0x11672F,  0x1103CA,  0x109E37,  0x10364B,  0xFCBDA,   0xF5EAE,   0xEEE8C,
   0xE7B2D,   0xE0444,   0xD8971,   0xD0A46,   0xC863F,   0xBFCBB,   0xB6CF4,   0xAD5F0,
   0xA366F,   0x98CC6,   0x8D6AD,   0x810DB,   0x73642,   0x63E62,   0x518A6,   0x39A39,
   0x0
};

/*
#define	RESOLUTION			1000000L
#define	RESOLUTION_FLOAT	1000000.0
#define	RESOLUTION_FLOATf	1000000.0f
#define	FLOAT_RESOLUTION	0.0000005
#define	FLOAT_RESOLUTIONf	0.0000005f
#define _XPI      3141592 // 3.1415926535897932384626433832795
#define XPI      fixed(true,_XPI)
#define _X2PI     6283185 // 6.283185307179586476925286766559

#define X2PI     fixed(true,_X2PI)
#define _XPIO2    1570796 // 1.5707963267948966192313216916398
#define XPIO2    fixed(true,_XPIO2)
#define _XPIO4    785398 // 0.78539816339744830961566084581988
#define XPIO4    fixed(true, _XPIO4)
#define _XLN_E    2718282 // 2.71828182845904523536
#define XLN_E    fixed(true,_XLN_E)
#define _XLN_10   2302585 // 2.30258509299404568402
#define XLN_10   fixed(true,_XLN_10)
#define sqrt_error   fixed(true, 1000) // 0.001
*/

inline fixed::fixed(void)
{
	m_nVal = 0;
}

inline fixed::fixed(int nVal)
{
  m_nVal = nVal << 16;
}

inline fixed::fixed(float floatVal)
{
  m_nVal = (int32_t)(floatVal * 65536.0 + (floatVal < 0 ? -0.5 : 0.5));
}

inline fixed::fixed(const fixed& fixedVal)
{
  m_nVal = fixedVal.m_nVal;
}

inline fixed::fixed(const fixed* fixedVal)
{
  m_nVal = fixedVal->m_nVal;
}

fixed::~fixed(void)
{
}

inline fixed& fixed::operator=(float floatVal)
{
  m_nVal = (int32_t)(floatVal * 65536.0 + (floatVal < 0 ? -0.5 : 0.5));
  return *this;
}

inline fixed& fixed::operator=(fixed fixedVal)
{
  m_nVal = fixedVal.m_nVal;
  return *this;
}

inline fixed& fixed::operator=(int intVal)
{
  m_nVal = intVal << 16;
  return *this;
}

inline bool fixed::equals(fixed b)
{
  return (m_nVal == b.m_nVal);
}

inline bool fixed::operator==(float floatVal)
{
  return (m_nVal == (int32_t)(floatVal * 65536.0 + (floatVal < 0 ? -0.5 : 0.5)));
}

inline bool fixed::operator==(fixed fixedVal)
{
  return (m_nVal == fixedVal.m_nVal);
}

inline bool fixed::operator==(int intVal)
{
  return (m_nVal == (intVal << 16));
}

inline bool fixed::operator!=(float floatVal)
{
  return (m_nVal != (int32_t)(floatVal * 65536.0 + (floatVal < 0 ? -0.5 : 0.5)));
}

inline bool fixed::operator!=(fixed fixedVal)
{
  return (m_nVal != fixedVal.m_nVal);
}

inline bool fixed::operator!=(int intVal)
{
  return (m_nVal != intVal << 16);
}

inline bool fixed::lessthan(fixed b)
{
  return (m_nVal < b.m_nVal);
}

inline bool fixed::operator<(float floatVal)
{
  return (m_nVal < (int32_t)(floatVal * 65536.0 + (floatVal < 0 ? -0.5 : 0.5)));
}

inline bool fixed::operator<(fixed fixedVal)
{
  return (m_nVal < fixedVal.m_nVal);
}

inline bool fixed::operator<(int intVal)
{
  return (m_nVal < intVal << 16);
}

inline bool fixed::lessthanequal(fixed b)
{
  return (m_nVal <= b.m_nVal);
}

inline bool fixed::operator<=(float floatVal)
{
  return (m_nVal <= (int32_t)(floatVal * 65536.0 + (floatVal < 0 ? -0.5 : 0.5)));
}

inline bool fixed::operator<=(fixed fixedVal)
{
  return (m_nVal <= fixedVal.m_nVal);
}

inline bool fixed::operator<=(int intVal)
{
  return (m_nVal <= intVal << 16);
}

inline bool fixed::operator>(float floatVal)
{
  return (m_nVal > (int32_t)(floatVal * 65536.0 + (floatVal < 0 ? -0.5 : 0.5)));
}

inline bool fixed::operator>(fixed fixedVal)
{
  return (m_nVal > fixedVal.m_nVal);
}

inline bool fixed::operator>(int intVal)
{
  return (m_nVal > intVal << 16);
}

inline bool fixed::operator>=(float floatVal)
{
  return (m_nVal >= (int32_t)(floatVal * 65536.0 + (floatVal < 0 ? -0.5 : 0.5)));
}

inline bool fixed::operator>=(fixed fixedVal)
{
  return (m_nVal >= fixedVal.m_nVal);
}

inline bool fixed::operator>=(int intVal)
{
  return (m_nVal >= intVal << 16);
}

inline fixed::operator float(void)
{
  return (float)m_nVal/65536.0;
}

inline fixed::operator int(void)
{
  if(m_nVal >= 0)
    return m_nVal >> 16;
  else
    return ~((~m_nVal) >> 16);
}

inline fixed fixed::floor(void)
{
  fixed f;
  if(m_nVal >= 0)
    f.m_nVal = m_nVal & ~0xFFFF;
  else
    f.m_nVal = ~((~m_nVal) & ~0xFFFF);
  return f;
}

inline fixed fixed::ceil(void)
{
  fixed f;
  if(m_nVal >= 0)
    f.m_nVal = ((m_nVal + 0xFFFF) & ~0xFFFF);
  else
    f.m_nVal = ~(~(m_nVal + 0xFFFF) & ~0xFFFF);
  return f;
}

inline fixed fixed::add(fixed b)
{
  fixed a;
  a.m_nVal = m_nVal + b.m_nVal;
  return a;
}

inline fixed fixed::operator+(fixed b)
{
  return add(b);
}

inline fixed fixed::subtract(fixed b)
{
  fixed a;
  a.m_nVal = m_nVal - b.m_nVal;
  return a;
}

inline fixed fixed::operator-(fixed b)
{
  return subtract(b);
}

inline fixed fixed::multiply(fixed b)
{
  fixed a;
  long long lx = m_nVal;
  long long ly = b.m_nVal;
  long long lres = (lx * ly);
  a.m_nVal = lres >> 16;
  return a;
}

inline fixed fixed::operator*(fixed b)
{
  return multiply(b);
}

inline fixed fixed::divide(fixed b)
{
  fixed a;
  long long lres = m_nVal;
  lres <<= 16;
  lres /= b.m_nVal;
  a.m_nVal = (int32_t)lres;
  return a;
}

inline fixed fixed::operator/(fixed b)
{
  return divide(b);
}

inline fixed fixed::add(float b)
{
  fixed _b = b;
  return add(_b);
}

inline fixed fixed::operator+(float b)
{
  return add(b);
}

inline fixed fixed::subtract(float b)
{
  fixed _b = b;
  return subtract(_b);
}

inline fixed fixed::operator-(float b)
{
  return subtract(b);
}

inline fixed fixed::multiply(float b)
{
  fixed _b = b;
  return multiply(_b);
}

inline fixed fixed::operator*(float b)
{
  return multiply(b);
}

inline fixed fixed::divide(float b)
{
  fixed _b = b;
  return divide(_b);
}

inline fixed fixed::operator/(float b)
{
  return divide(b);
}

inline fixed fixed::add(int b)
{
  fixed _b = b;
  return add(_b);
}

inline fixed fixed::operator+(int b)
{
  return add(b);
}

inline fixed fixed::subtract(int b)
{
  fixed _b = b;
  return subtract(_b);
}

inline fixed fixed::operator-(int b)
{
  return subtract(b);
}

inline fixed fixed::multiply(int b)
{
  fixed _b = b;
  return multiply(_b);
}

inline fixed fixed::operator*(int b)
{
  return multiply(b);
}

inline fixed fixed::divide(int b)
{
  fixed _b = b;
  return divide(_b);
}

inline fixed fixed::operator/(int b)
{
  return divide(b);
}

inline fixed operator-(float a, fixed b)
{
  fixed _a = a;
  return _a - b;
}

inline fixed operator-(int a, fixed b)
{
  fixed _a = a;
  return _a - b;
}

inline bool operator<(float b, fixed a)
{
  return a >= b;
}

inline bool operator<(int b, fixed a)
{
  return a >= b;
}

inline fixed operator-(fixed a)
{
  return 0-a;
}

inline fixed fixed::cos() 
{
  fixed r;
  r.m_nVal = cos_tbl[((m_nVal + 0x4000) >> 15) & 0x1FF];
  return r;
}

inline fixed fixed::sin() 
{
  fixed r;
  r.m_nVal = cos_tbl[((m_nVal - 0x400000 + 0x4000) >> 15) & 0x1FF];
  return r;
}

inline fixed fixed::tan() 
{
  fixed r;
  r.m_nVal = tan_tbl[((m_nVal + 0x4000) >> 15) & 0xFF];
  return r;
}

inline fixed fixed::acos()
{
  fixed r;
  r.m_nVal = acos_tbl[(m_nVal + 65536 + 127) >> 8];
  return r;
}

inline fixed fixed::asin()
{
  fixed r;
  r.m_nVal = 0x00400000 - acos_tbl[(m_nVal + 65536 + 127) >> 8];
  return r;
}

inline fixed fixed::atan()
{
  fixed r;

  int a, b, c;            /* for binary search */
  int32_t d;                /* difference value for search */

  if (m_nVal >= 0) {           /* search the first part of tan table */
	a = 0;
	b = 127;
  }
  else {                  /* search the second half instead */
	a = 128;
	b = 255;
  }

  do {
	c = (a + b) >> 1;
	d = m_nVal - tan_tbl[c];

	if (d > 0)
	  a = c + 1;
	else if (d < 0)
	    b = c - 1;

  } while ((a <= b) && (d != 0));

  if (m_nVal >= 0)
	r.m_nVal = ((long)c) << 15;
  else
	r.m_nVal = (-0x00800000L + (((long)c) << 15));

  return r;
}

float sin(fixed a) 
{
  return (float)a.sin();
}

float cos(fixed a) 
{
  return (float)a.cos();
}

float tan(fixed a) 
{
  return (float)a.tan();
}

float asin(fixed a) 
{
  return (float)a.asin();
}

float acos(fixed a) 
{
  return (float)a.acos();
}

float atan(fixed a) 
{
  return (float)a.atan();
}

#endif // _FIXED_H
