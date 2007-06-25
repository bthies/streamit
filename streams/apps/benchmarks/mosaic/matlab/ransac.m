%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% RANSAC - Robustly fits a model to data with the RANSAC algorithm
%
% Usage:
%
% [M, inliers] = ransac(x, fittingfn, distfn, degenfn s, t)
%
% Arguments:
%     x         - Data sets to which we are seeking to fit a model M
%                 It is assumed that x is of size [d x Npts]
%                 where d is the dimensionality of the data and Npts is
%                 the number of data points.
%     fittingfn - Handle to a function that fits a model to s
%                 data from x.  It is assumed that the function is of the
%                 form: 
%                    M = fittingfn(x)
%     distfn    - Handle to a function that evaluates the
%                 distances from the model to data x.
%                 It is assumed that the function is of the form:
%                    d = distfn(M, x)
%                 where d is a vector of distances of length Npts.
%     degenfn   - Handle to a function that determines whether a
%                 set of datapoints will produce a degenerate model.
%                 This is used to discard random samples that do not
%                 result in useful models.
%                 It is assumed that degenfn is a boolean function of
%                 the form: 
%                    r = degenfn(x)
%     s         - The minimum number of samples from x required by
%                 fittingfn to fit a model.
%     t         - The distance threshold between data point and the model
%                 used to decide whether a point is an inlier or not.
%
% Returns:
%     M       - The model having the greatest number of inliers.
%     inliers - An array of indices of the elements of x that were
%               the inliers for the best model.
%
% Bharath Kalyan
% Last Modified: 10-17-2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


function [ransacWorks, M, inliers] = ransac(x, fittingfn, distfn, degenfn, s, t)
    
    [rows, npts] = size(x);                
    ransacWorks = 1;
    p = 0.99;    % Desired probability of choosing at least one sample
                 % free from outliers

    maxTrials = 1000;    % Maximum number of trials before we give up.
    maxDataTrials = 100; % Max number of attempts to select a non-degenerate
                         % data set.
    
    bestM = NaN;      % Sentinel value allowing detection of solution failure.
    trialcount = 0;
    bestscore =  0;    
    N = 1;            % Dummy initialisation for number of trials.
    
    while N > trialcount
        
        % Select at random s datapoints to form a trial model, M.
        % In selecting these points we have to check that they are not in
        % a degenerate configuration.
        degenerate = 1;
        count = 1;
        while degenerate
            % Generate s random indicies in the range 1..npts
            ind = ceil(rand(1,s)*npts);
            
            % Test that these points are not a degenerate configuration.
            degenerate = feval(degenfn, x(:,ind));

            % Safeguard against being stuck in this loop forever
            count = count + 1;
            if count > maxDataTrials
                warning('Unable to select a nondegenerate data set');
                break
            end
        end
        
        % Fit model to this random selection of data points.
        M = feval(fittingfn, x(:,ind));
        
        % Evaluate distances between points and model.
        d = feval(distfn, M, x);
        
        % Find the indices of points that are inliers to this model.
        inliers = find(abs(d) < t);
        ninliers = length(inliers);
        
        if ninliers > bestscore    % Largest set of inliers so far...
            bestscore = ninliers;  % Record data for this model
            bestinliers = inliers;
            bestM = M;
            
            % Update estimate of N, the number of trials to ensure we pick, 
            % with probability p, a data set with no outliers.
            fracinliers =  ninliers/npts;
            pNoOutliers = 1 -  fracinliers^s;
            pNoOutliers = max(eps, pNoOutliers);  % Avoid division by -Inf
            pNoOutliers = min(1-eps, pNoOutliers);% Avoid division by 0.
            N = log(1-p)/log(pNoOutliers);
        end
        
        trialcount = trialcount+1;
        %fprintf('trial %d out of %d         \r',trialcount, ceil(N));

        % Safeguard against being stuck in this loop forever
        if trialcount > maxTrials
            warning( ...
            sprintf('ransac reached the maximum number of %d trials',...
                    maxTrials));
            break
        end     
    end
    fprintf('\n');
    
    if ~isnan(bestM)   % We got a solution 
        M = bestM;
        inliers = bestinliers;
        trialcount
    else           
        sprintf('ransac was unable to find a useful solution');
        M=ones(3);
        inliers=[1:9];
        ransacWorks=0 ;     
    end
    