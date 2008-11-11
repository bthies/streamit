
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <vec_ops.h>

#ifndef _FIXED_H
#define _FIXED_H

#define PI_FLOAT 3.1415

class fixed {
private:
  //int32_t m_nVal;
public:
  int32_t m_nVal;

  fixed(void);
  fixed(int nVal);
  fixed(float nVal);
  fixed(double nVal);
  fixed(const fixed& fixedVal);
  fixed(const volatile fixed& fixedVal);
  fixed(const fixed* fixedVal);
  ~fixed(void);

  fixed& operator=(float floatVal);
  fixed& operator=(int intVal);
  volatile fixed& operator=(fixed fixedVal) volatile;

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
  fixed absolute(void);

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

fixed sin(fixed a);
fixed cos(fixed a);
fixed tan(fixed a);
fixed asin(fixed a);
fixed acos(fixed a);
fixed atan(fixed a);

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
int32_t cos_tbl[513] =
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
  65220,  65294,  65358,  65413,  65457,  65492,  65516,  65531,
  65536
};

/* precalculated fixed point (16.16) tangents for a half circle (0-127) */
int32_t tan_tbl[257] =
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
  -6455,  -5644,  -4834,  -4026,  -3220,  -2414,  -1609,  -804,
  0
};

/* precalculated fixed point (16.16) inverse cosines (-1 to 1) */
int32_t acos_tbl[513] =
{
  205887, 200093, 197690, 195844, 194287, 192914, 191671, 190526, 
  189460, 188458, 187509, 186606, 185742, 184912, 184113, 183342, 
  182595, 181870, 181165, 180479, 179810, 179157, 178519, 177895, 
  177283, 176683, 176095, 175517, 174949, 174391, 173842, 173301, 
  172768, 172243, 171725, 171215, 170711, 170213, 169722, 169237, 
  168757, 168283, 167814, 167350, 166891, 166436, 165987, 165541, 
  165100, 164663, 164230, 163800, 163375, 162953, 162534, 162119, 
  161708, 161299, 160894, 160491, 160092, 159695, 159302, 158911, 
  158522, 158137, 157753, 157373, 156994, 156618, 156245, 155873, 
  155504, 155137, 154772, 154409, 154048, 153689, 153331, 152976, 
  152623, 152271, 151921, 151573, 151227, 150882, 150539, 150197, 
  149857, 149518, 149181, 148846, 148512, 148179, 147848, 147518, 
  147189, 146862, 146536, 146211, 145888, 145565, 145244, 144924, 
  144606, 144288, 143972, 143656, 143342, 143029, 142717, 142405, 
  142095, 141786, 141478, 141171, 140865, 140559, 140255, 139951, 
  139649, 139347, 139046, 138746, 138447, 138149, 137851, 137554, 
  137258, 136963, 136669, 136375, 136082, 135790, 135498, 135207, 
  134917, 134628, 134339, 134051, 133763, 133476, 133190, 132904, 
  132620, 132335, 132051, 131768, 131485, 131203, 130922, 130641, 
  130360, 130081, 129801, 129522, 129244, 128966, 128689, 128412, 
  128136, 127860, 127584, 127309, 127035, 126761, 126487, 126214, 
  125941, 125668, 125396, 125125, 124854, 124583, 124312, 124042, 
  123773, 123503, 123234, 122966, 122697, 122430, 122162, 121895, 
  121628, 121361, 121095, 120829, 120563, 120298, 120033, 119768, 
  119503, 119239, 118975, 118711, 118448, 118185, 117922, 117659, 
  117397, 117134, 116872, 116611, 116349, 116088, 115827, 115566, 
  115305, 115044, 114784, 114524, 114264, 114004, 113745, 113485, 
  113226, 112967, 112708, 112449, 112190, 111932, 111674, 111415, 
  111157, 110899, 110641, 110384, 110126, 109869, 109611, 109354, 
  109097, 108840, 108583, 108326, 108069, 107812, 107556, 107299, 
  107042, 106786, 106530, 106273, 106017, 105761, 105504, 105248, 
  104992, 104736, 104480, 104224, 103968, 103712, 103456, 103200, 
  102944, 102688, 102432, 102176, 101920, 101664, 101408, 101151, 
  100895, 100639, 100383, 100127, 99871, 99614, 99358, 99102, 
  98845, 98589, 98332, 98075, 97818, 97562, 97305, 97048, 
  96791, 96533, 96276, 96019, 95761, 95504, 95246, 94988, 
  94730, 94472, 94214, 93956, 93697, 93438, 93180, 92921, 
  92662, 92402, 92143, 91883, 91624, 91364, 91103, 90843, 
  90583, 90322, 90061, 89800, 89538, 89277, 89015, 88753, 
  88491, 88228, 87966, 87703, 87439, 87176, 86912, 86648, 
  86384, 86120, 85855, 85590, 85324, 85059, 84793, 84526, 
  84260, 83993, 83725, 83458, 83190, 82922, 82653, 82384, 
  82115, 81845, 81575, 81305, 81034, 80763, 80491, 80219, 
  79947, 79674, 79401, 79127, 78853, 78578, 78303, 78028, 
  77752, 77475, 77199, 76921, 76643, 76365, 76086, 75807, 
  75527, 75247, 74966, 74684, 74402, 74119, 73836, 73552, 
  73268, 72983, 72697, 72411, 72124, 71837, 71549, 71260, 
  70970, 70680, 70389, 70098, 69806, 69513, 69219, 68924, 
  68629, 68333, 68036, 67739, 67440, 67141, 66841, 66540, 
  66239, 65936, 65633, 65328, 65023, 64717, 64409, 64101, 
  63792, 63482, 63171, 62859, 62545, 62231, 61916, 61599, 
  61282, 60963, 60643, 60322, 60000, 59676, 59352, 59026, 
  58698, 58370, 58040, 57708, 57376, 57042, 56706, 56369, 
  56031, 55691, 55349, 55006, 54661, 54314, 53966, 53616, 
  53265, 52911, 52556, 52199, 51840, 51479, 51116, 50751, 
  50384, 50014, 49643, 49269, 48893, 48515, 48134, 47751, 
  47365, 46977, 46586, 46192, 45796, 45396, 44994, 44588, 
  44180, 43768, 43353, 42935, 42513, 42087, 41658, 41225, 
  40788, 40346, 39901, 39451, 38997, 38538, 38074, 37605, 
  37130, 36651, 36165, 35674, 35176, 34673, 34162, 33644, 
  33119, 32587, 32046, 31496, 30938, 30370, 29793, 29204, 
  28604, 27993, 27368, 26730, 26077, 25408, 24722, 24018, 
  23293, 22546, 21774, 20975, 20145, 19281, 18378, 17429, 
  16427, 15361, 14217, 12974, 11600, 10043, 8197, 5795, 
  0
};

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

inline fixed::fixed(double floatVal)
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

inline fixed::fixed(const volatile fixed& fixedVal)
{
  m_nVal = fixedVal.m_nVal;
}

fixed::~fixed(void)
{
}

inline fixed& fixed::operator=(float floatVal)
{
  m_nVal = (int32_t)(floatVal * 65536.0 + (floatVal < 0 ? -0.5 : 0.5));
  return *this;
}

inline volatile fixed& fixed::operator=(fixed fixedVal) volatile
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

inline fixed fixed::absolute(void)
{
  fixed f(this);
  if (f.m_nVal < 0)
    f.m_nVal = -f.m_nVal;
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
  int32_t r0, r1, r2, r3, r4;

  r0 = m_nVal;
  r1 = b.m_nVal;

  r2 = __insn_mulhl_su(r0, r1);
	r2 = __insn_mulhla_su(r2, r1, r0);
	r4 = __insn_mulll_uu(r0, r1);
  r4 = __insn_srai(r4, 16);
  r3 = __insn_mulhh_ss(r0, r1);
  r3 = __insn_shli(r3, 16);
  r2 = __insn_add(r2, r4);
  r0 = __insn_add(r2, r3);

  fixed a;
  a.m_nVal = r0;

  /*
  long long lx = m_nVal;
  long long ly = b.m_nVal;
  long long lres = (lx * ly);
  a.m_nVal = lres >> 16;
  */

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
  r.m_nVal = cos_tbl[(int)((m_nVal % 0x6487E) / 0x324)];
  return r;
}

inline fixed fixed::sin() 
{
  fixed r;
  r.m_nVal = cos_tbl[(int)(((m_nVal + 0x1921F) % 0x6487E) / 0x324)];
  return r;
}

inline fixed fixed::tan() 
{
  fixed r;
  r.m_nVal = tan_tbl[(int)((m_nVal % 0x3243F) / 0x324)];
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

#if 0

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
	b = 256;
  }

  do {
	c = (a + b) >> 1;
	d = m_nVal - tan_tbl[c];

	if (d > 0)
	  a = c + 1;
	else if (d < 0)
	    b = c - 1;

  } while ((a <= b) && (d != 0));

  /*
  if (m_nVal >= 0)
	r.m_nVal = ((long)c) << 15;
  else
	r.m_nVal = (-0x00800000L + (((long)c) << 15));
  */

  if (m_nVal >= 0)
	r.m_nVal = c * 0x324;
  else
	r.m_nVal = -((c - 128) * 0x324);

  return r;
}

#else

// Atan implementation based on code at: http://dspguru.com/comp.dsp/tricks/alg/fxdatan2.htm
// This has a max error of 0.07 rads.  It can be improved to provide 7x more
// accuracy with a few more operations (see website).
inline fixed fixed::atan()
{
   fixed coeff(-PI_FLOAT / 2);

   fixed abs_y = this->absolute();
   fixed r = (1 - abs_y) / (abs_y + 1);
   fixed angle = coeff * r;

   if (*this < 0)
     return(-angle);     
   else
     return(angle);
}
#endif

inline fixed sin(fixed a) 
{
  return a.sin();
}

inline fixed cos(fixed a) 
{
  return a.cos();
}

inline fixed tan(fixed a) 
{
  return a.tan();
}

inline fixed asin(fixed a) 
{
  return a.asin();
}

inline fixed acos(fixed a) 
{
  return a.acos();
}

inline fixed atan(fixed a) 
{
  return a.atan();
}

inline fixed operator*(float a, fixed b)
{
  return b.multiply(a);
}

inline fixed operator+(float a, fixed b)
{
  return b.add(a);
}

inline fixed operator/(float a, fixed b)
{
  return b.divide(a);
}


inline fixed operator*(int a, fixed b)
{
  return b.multiply(a);
}

inline fixed operator+(int a, fixed b)
{
  return b.add(a);
}

inline fixed operator/(int a, fixed b)
{
  return b.divide(a);
}


#endif // _FIXED_H
