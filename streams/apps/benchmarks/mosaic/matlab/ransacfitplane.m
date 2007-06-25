%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% RANSACFITPLANE - fits plane to 3D array of points using RANSAC
%
% Usage  [B, P, inliers] = ransacfitplane(XYZ, t)
%
% This function uses the RANSAC algorithm to robustly fit a plane
% to a set of 3D data points.
%
% Arguments:
%          XYZ - 3xNpts array of xyz coordinates to fit plane to.
%          t   - The distance threshold between data point and the plane
%                used to decide whether a point is an inlier or not.
%
% Returns:
%           B - 4x1 array of plane coefficients in the form
%               b(1)*X + b(2)*Y +b(3)*Z + b(4) = 0
%               The magnitude of B is 1.
%               This plane is obtained by a least squares fit to all the
%               points that were considered to be inliers, hence this
%               plane will be slightly different to that defined by P below.
%           P - The three points in the data set that were found to
%               define a plane having the most number of inliers.
%               The three columns of P defining the three points.
%           inliers - The indices of the points that were considered
%                     inliers to the fitted plane.
%
% Bharath Kalyan
% Last Modified: 10-17-2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [B, P, inliers] = ransacfitplane(XYZ, t)
    
    [rows, npts] = size(XYZ);
    
    if rows ~=3
        error('data is not 3D');
    end
    
    if npts < 3
        error('too few points to fit plane');
    end
    
    s = 3;  % Minimum No of points needed to fit a plane.
        
    fittingfn = @defineplane;
    distfn    = @planeptdist;
    degenfn   = @isdegenerate;

    [P, inliers] = ransac(XYZ, fittingfn, distfn, degenfn, s, t);
    
    % Perform least squares fit to the inlying points
    B = fitplane(XYZ(:,inliers));
    
%------------------------------------------------------------------------
% Function to define a plane given 3 data points as required by
% RANSAC. In our case we use the 3 points directly to define the plane.

function P = defineplane(X);
    P = X;
    
%------------------------------------------------------------------------
% Function to calculate distances between a plane and a an array of points.
% The plane is defined by a 3x3 matrix, P.  The three columns of P defining
% three points that are within the plane.

function d = planeptdist(P, X)
    
    n = cross(P(:,2)-P(:,1), P(:,3)-P(:,1)); % Plane normal.
    n = n/norm(n);                           % Make it a unit vector.
    
    npts = length(X);
    d = zeros(npts,1);
    for i = 1:npts
        d(i) = abs(dot(X(:,i)-P(:,1), n));    % Distance
    end
    
%------------------------------------------------------------------------
% Function to determine whether a set of 3 points are in a degenerate
% configuration for fitting a plane as required by RANSAC.  In this case
% they are degenerate if they are colinear.

function r = isdegenerate(X)
    
    % The three columns of X are the coords of the 3 points.
    r = iscolinear(X(:,1),X(:,2),X(:,3));