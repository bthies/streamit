%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Demonstration of feature matching via simple correlation, and then using
% RANSAC to estimate the fundamental matrix and at the same time identify
% (mostly) inlying matches

% Bharath Kalyan
% Last Modified: 10-17-2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [ransacWorks,featTime, corrTime, ransacTime, sumTime, testfuncTime] = testfunc(file1,file2)

    close all    
    testfuncStart = clock;
%   for kk = 5000:5022

    currentDir=pwd;
    thresh = 3000;   % Harris corner threshold
    nonmaxrad = 3;  % Non-maximal suppression radius
%    imagefile1 = ['D:\Research\Correlation\images\image' int2str(kk) '.bmp'];
%    imagefile2 = ['D:\Research\Correlation\images\image' int2str(kk+5) '.bmp'];
%    scanfile1 = ['D:\Research\Correlation\images\scan' int2str(kk) '.txt'];
%    scanfile2 = ['D:\Research\Correlation\images\scan' int2str(kk+5) '.txt'];
 
    ransacWorks = 1;
    cd C:\Do'cuments and Settings'\basier\desktop\images\ppmsFromVid2\;
 
    im1 = imread(sprintf('%d.ppm',file1));
    im2 = imread(sprintf('%d.ppm',file2));
    cd (currentDir);
    im1 = rgb2gray(im1);
    im2 = rgb2gray(im2);
   
%    row_col1 = load(scanfile1);
%    row_col2 = load(scanfile2);
    
%    r1 = row_col1(:,1);
%    c1 = row_col1(:,2);
 
%    r2 = row_col2(:,1);
%    c2 = row_col2(:,2);

    % Find Harris corners in image1 and image2  
    featureStart = clock;
    [cim1, r1, c1] = harris(im1, 1, thresh, 3);
    %show(im1,1), hold on, plot(c1,r1,'r+');
    [cim2, r2, c2] = harris(im2, 1, thresh, 3);
    %show(im2,2), hold on, plot(c2,r2,'r+');
    featureStop = clock;
    
    %drawnow

    corrStart = clock;
    w = 13;    % Window size for correlation matching
    [m1,m2] = correlation(im1, [r1';c1'], im2, [r2';c2'], w);
    corrStop = clock;
    
    % Display putative matches
    %show(im1,3), set(3,'name','Putative matches'), hold on    
    %for n = 1:length(m1);
	%line([m1(2,n) m2(2,n)], [m1(1,n) m2(1,n)])
    %end

    % Assemble homogeneous feature coordinates for fitting of the
    % fundamental matrix, note that [x,y] corresponds to [col, row]
    x1 = [m1(2,:); m1(1,:); ones(1,length(m1))];
    x2 = [m2(2,:); m2(1,:); ones(1,length(m1))];    
    
    ransacStart = clock;
    t = .001;  % Distance threshold for deciding outliers
    [ransacWorks, F, inliers] = ransacfit(x1, x2, t);
    ransacStop = clock;
    
    
    testfuncStop = clock;
    % Display both images overlayed with inlying matched feature points
    %show(double(im1)+double(im2),4), set(4,'name','Inlying matches'), hold on 
    
    %plot(m1(2,inliers),m1(1,inliers),'r+');
    %plot(m2(2,inliers),m2(1,inliers),'g+');
    
    featTime=etime(featureStop, featureStart);
    corrTime=etime(corrStop, corrStart);
    ransacTime=etime(ransacStop, ransacStart);
    sumTime=featTime+corrTime+ransacTime;
    testfuncTime=etime(testfuncStop, testfuncStart);
    %sprintf('feature:       %f',featTime)
    %sprintf('correlation:   %f',corrTime)
    %sprintf('ransac:        %f',ransacTime)
    %sprintf('sum:           %f',sumTime)
    %sprintf('testfunc:      %f',testfuncTime)
    %for n = inliers
	%line([m1(2,n) m2(2,n)], [m1(1,n) m2(1,n)],'color',[0 0 1])
    %end
    
    %pause;
end