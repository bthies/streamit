function clocking(iters)
    sniffle=zeros(1,5);
    for n = 1:iters
     works = 0;
     while not(works)
         [works,a,b,c,d,e] = testfunc(n-1,n);    
     end
     sprintf('n = %d',n)
     sniffle=sniffle + [a,b,c,d,e];
    end    
    sniffle/iters