for o=1:100,
  for p=1:33,
    for q=1:8,
      temp = floor(good((o-1)*33+p) / (2^(8-q)));
      if (rem(temp,2))
	separated1((p-1)*8+q) = 1;
      else 
	separated1((p-1)*8+q) = 0;
      endif
    end
  end
  separated((o-1)*260+1:o*260) = separated1(5:length(separated1));
end
