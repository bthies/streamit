%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% SHOW - Displays an image with the right size and colors and with a title.
%
% Usage:  show(im, figNo)
%
% Arguments:  im    - Either a 2 or 3D array of pixel values or the name
%                     of an image file;
%             figNo - Optional figure number to display image in. If
%                     figNo is 0 the current figure or subplot is assumed.
% Bharath Kalyan
% Last Modified: 10-17-2004

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function show(im, figNo)
    warning off            % Turn off warnings that might arise if image
                           % has to be rescaled to fit on screen
			   
    if ~isnumeric(im) & ~islogical(im) % Guess that an image name has been supplied
	Title = im;
	im = imread(im);
    else
	Title = inputname(1);  % Get variable name of image data
    end
    
    newWindow = 1;
    if nargin == 2
	if figNo               % We have a valid figure number
	    figure(figNo);     % Reuse or create a figure window with this number
            subplot('position',[0 0 1 1]); % Use the whole window
	else                   
	    newWindow=0;       % figNo == 0
	end
    else
	figNo = figure;        % Create new figure window
	subplot('position',[0 0 1 1]); % Use the whole window
    end
    
    if ndims(im) == 2          % Display as greyscale
	imagesc(im)
	colormap('gray')
    else
	imshow(im)             % Display as RGB
    end

    if newWindow            
	axis image, axis off, set(figNo,'name', ['  ' Title]), truesize(figNo)
    else                                   % Assume we are trying to do a subplot
	axis image, axis off, title(Title) % Use a title rather than rename the figure	
    end

    warning on  % Restore warnings