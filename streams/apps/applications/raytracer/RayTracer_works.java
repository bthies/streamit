/**
 * This version runs in the Java library.  It doesn't seem to have
 * intelligble output though.
 */

import streamit.library.*;
import streamit.library.io.*;

class Complex extends Structure {
  public float real;
  public float imag;
}
class Vector3 extends Structure {
  float x;
  float y;
  float z;
}
class Ray extends Structure {
  Vector3 o;
  Vector3 d;
}
class Color extends Structure {
  float r;
  float g;
  float b;
}
class HitInfo extends Structure {
  float t;
  Color c;
  Vector3 pos;
  Vector3 norm;
}
class RaySource extends Filter
{
  int i, j;
  int nx;
  int ny;
  public void work() {
    Ray r;
    r = new Ray();
    r.o = new Vector3();
    r.d = new Vector3();
    if (i == j && j == 0) {
      System.out.println("P6\n32 32\n255\n");
    }
    r.o.x = 0;
    r.o.y = 0;
    r.o.z = 0;
    r.d.x = ((i - (nx / 2.0f)) / nx);
    r.d.y = ((j - (ny / 2.0f)) / ny);
    r.d.z = -1;
    j++;
    if ((j == ny)) {
      i++;
      j = 0;
      if ((i == nx)) {
        i = 0;
      };
    };
    output.push(r);
  }
  public RaySource(int nx, int ny) {
    super(nx, ny);
  }
  public void init(final int nx, final int ny) {
    this.nx = nx;
    this.ny = ny;
    output = new Channel(Ray.class, 1);
    i = 0;
    j = 0;
  }
}
class CircleIntersect extends Filter
{
  Vector3 center;
  float radius;
  Color diffuse;
  float x;
  float y;
  float z;
  float r;
  float cr;
  float cg;
  float cb;
  public void work() {
    Ray r;
    r = new Ray();
    r.o = new Vector3();
    r.d = new Vector3();
    float A, B, C;
    float t;
    float discrim;
    Vector3 o_minus_c;
    o_minus_c = new Vector3();
    HitInfo hi;
    hi = new HitInfo();
    hi.c = new Color();
    hi.pos = new Vector3();
    hi.norm = new Vector3();
    Ray __temp_var_0;
    __temp_var_0 = new Ray();
    __temp_var_0.o = new Vector3();
    __temp_var_0.d = new Vector3();
    __temp_var_0 = (Ray)input.pop();
    r = __temp_var_0;
    o_minus_c.x = (r.o.x - center.x);
    o_minus_c.y = (r.o.y - center.y);
    o_minus_c.z = (r.o.z - center.z);
    A = (((r.d.x * r.d.x) + (r.d.y * r.d.y)) + (r.d.z * r.d.z));
    B = (((r.d.x * o_minus_c.x) + (r.d.y * o_minus_c.y)) + (r.d.z * o_minus_c.z));
    C = ((((o_minus_c.x * o_minus_c.x) + (o_minus_c.y * o_minus_c.y)) + (o_minus_c.z * o_minus_c.z)) - (radius * radius));
    discrim = ((B * B) - (A * C));
    if ((discrim <= 0)) {
      hi.t = -1;
      hi.c.r = 0;
      hi.c.g = 0;
      hi.c.b = 0;
      output.push(hi);
    } else {
      discrim = (float)Math.sqrt(discrim);
      hi.t = ((-B - discrim) / A);
      hi.c = diffuse;
      output.push(hi);
    };
  }
  public CircleIntersect(float x, float y, float z, float r, float cr, float cg, float cb) {
    super(x, y, z, r, cr, cg, cb);
  }
  public void init(final float x, final float y, final float z, final float r, final float cr, final float cg, final float cb) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.r = r;
    this.cr = cr;
    this.cg = cg;
    this.cb = cb;
    center = new Vector3();
    diffuse = new Color();
    input = new Channel(Ray.class, 1);
    output = new Channel(HitInfo.class, 1);
    center.x = x;
    center.y = y;
    center.z = z;
    radius = r;
    diffuse.r = cr;
    diffuse.g = cg;
    diffuse.b = cb;
  }
}
class Lighting extends Filter
{
  public void work() {
    HitInfo h;
    h = new HitInfo();
    h.c = new Color();
    h.pos = new Vector3();
    h.norm = new Vector3();
    HitInfo __temp_var_1;
    __temp_var_1 = new HitInfo();
    __temp_var_1.c = new Color();
    __temp_var_1.pos = new Vector3();
    __temp_var_1.norm = new Vector3();
    __temp_var_1 = (HitInfo)input.pop();
    h = __temp_var_1;
    output.push(h.c);
  }
  public void init() {
    input = new Channel(HitInfo.class, 1);
    output = new Channel(Color.class, 1);
  }
}
class WritePPM extends Filter
{
  int nx;
  int ny;
  public void work() {
    Color c;
    c = new Color();
    Color __temp_var_2;
    __temp_var_2 = new Color();
    __temp_var_2 = (Color)input.pop();
    c = __temp_var_2;
    /*System.out.println(c.r);*/
    char r, g, b;
    r = (char) (c.r * 255);
    g = (char) (c.g * 255);
    b = (char) (c.b * 255);
    System.out.print(r);
    System.out.print(g);
    System.out.print(b);
  }
  public WritePPM(int nx, int ny) {
    super(nx, ny);
  }
  public void init(final int nx, final int ny) {
    this.nx = nx;
    this.ny = ny;
    input = new Channel(Color.class, 1);
  }
}
public class RayTracer_works extends StreamIt
{
  public static void main(String[] args) {
    RayTracer program = new RayTracer();
    program.run(args);
  }
  public void init() {
    int nx, ny;
    Vector3 v;
    v = new Vector3();
    Color c;
    c = new Color();
    nx = 32;
    ny = 32;
    v.x = 0;
    v.y = 0;
    v.z = -2;
    c.r = 1;
    c.g = 0;
    c.b = 0;
    add(new RaySource(nx, ny));
    add(new CircleIntersect(0, 0, -2, 0.5f, 1.0f, 0.0f, 0.0f));
    add(new Lighting());
    add(new WritePPM(nx, ny));
  }
}
