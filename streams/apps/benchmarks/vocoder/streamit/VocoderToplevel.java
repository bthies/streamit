class VocoderToplevel extends StreamIt implements Constants {
  public static void main(String args[]) {
    new VocoderToplevel().run(args);
  }

  public void init() {
//      add(new FileReader("test2.wav", Short.TYPE));
//  //      add(new ShortPrinter());
//      add(new ShortToFloat());

    add(new StepSource(100));
    add(new IntToFloat());
    add(new Delay(DFT_LENGTH_NOM));
//      add(new Delay(DFT_LENGTH*2));


    add(new Vocoder());

    add(new InvDelay((DFT_LENGTH -2) * m_LENGTH / n_LENGTH));
//      add(new InvDelay(DFT_LENGTH * m_LENGTH / n_LENGTH));

    /**
    add(new TransformBank(8, 8));
//      add(new TransformBank(100*m_LENGTH / n_LENGTH, 200));
//      add(new RectangularToPolar());
//      add(new ComplexPrinter(100 * m_LENGTH / n_LENGTH));
//      add(new FloatPrinter());
    add(new FloatVoid());

    /**/
    add(new FloatToShort());

//      add(new Timer(476672)); //total number of samples
//      add(new Timer(524288));
    add(new ShortPrinter());
    add(new ShortVoid());

//        add(new FileWriter("test3.wav", Short.TYPE));
       /**/
  }
}
