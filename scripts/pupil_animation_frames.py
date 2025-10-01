#!/usr/bin/env python3
"""
Glyph Toy Eyes Animation Frame Generator
Creates frames for pupil movement: right → center → left → center
"""

import math

def create_eye_frame(left_pupil_offset_x=0, right_pupil_offset_x=0, left_pupil_offset_y=0, right_pupil_offset_y=0):
    """
    Create a 25x25 frame with eyes and pupils at specified offsets
    """
    # Initialize 25x25 matrix with zeros
    frame = [[0 for _ in range(25)] for _ in range(25)]
    
    # Eye parameters from GlyphEyesService.kt
    eye_center_left = (8, 12)
    eye_center_right = (17, 12)
    eye_radius_x = 4
    eye_radius_y = 6
    pupil_radius = 2
    
    # Calculate actual pupil positions
    left_pupil_center = (eye_center_left[0] + left_pupil_offset_x, eye_center_left[1] + left_pupil_offset_y)
    right_pupil_center = (eye_center_right[0] + right_pupil_offset_x, eye_center_right[1] + right_pupil_offset_y)
    
    # Helper function to check if point is inside ellipse
    def is_inside_ellipse(x, y, center_x, center_y, radius_x, radius_y):
        dx = x - center_x
        dy = y - center_y
        return (dx * dx) / (radius_x * radius_x) + (dy * dy) / (radius_y * radius_y) <= 1.0
    
    # Helper function to check if point is inside circle
    def is_inside_circle(x, y, center_x, center_y, radius):
        dx = x - center_x
        dy = y - center_y
        return dx * dx + dy * dy <= radius * radius
    
    # Fill the frame
    for y in range(25):
        for x in range(25):
            pixel_value = 0
            
            # Check if pixel is inside left eye (white part)
            if is_inside_ellipse(x, y, eye_center_left[0], eye_center_left[1], eye_radius_x, eye_radius_y):
                pixel_value = 2040  # White eye
                
                # Check if pixel is inside left pupil (black part)
                if is_inside_circle(x, y, left_pupil_center[0], left_pupil_center[1], pupil_radius):
                    pixel_value = 0  # Black pupil
            
            # Check if pixel is inside right eye (white part)
            elif is_inside_ellipse(x, y, eye_center_right[0], eye_center_right[1], eye_radius_x, eye_radius_y):
                pixel_value = 2040  # White eye
                
                # Check if pixel is inside right pupil (black part)
                if is_inside_circle(x, y, right_pupil_center[0], right_pupil_center[1], pupil_radius):
                    pixel_value = 0  # Black pupil
            
            # Check if pixel is in the connection area between eyes
            elif (eye_center_left[0] + eye_radius_x <= x <= eye_center_right[0] - eye_radius_x and 
                  abs(y - eye_center_left[1]) <= eye_radius_y // 2):
                pixel_value = 2040  # White connection
            
            frame[y][x] = pixel_value
    
    return frame

def frame_to_csv_line(frame):
    """Convert 25x25 frame to CSV line (625 values)"""
    values = []
    for row in frame:
        for value in row:
            values.append(str(value))
    return ','.join(values)

def main():
    """Generate animation frames"""
    print("Generating Glyph Toy Eyes Animation Frames...")
    
    # Frame 1: Pupils looking right (2 pixels)
    frame1 = create_eye_frame(left_pupil_offset_x=2, right_pupil_offset_x=2)
    
    # Frame 2: Pupils center
    frame2 = create_eye_frame(left_pupil_offset_x=0, right_pupil_offset_x=0)
    
    # Frame 3: Pupils looking left (2 pixels)
    frame3 = create_eye_frame(left_pupil_offset_x=-2, right_pupil_offset_x=-2)
    
    # Frame 4: Pupils center again
    frame4 = create_eye_frame(left_pupil_offset_x=0, right_pupil_offset_x=0)
    
    frames = [frame1, frame2, frame3, frame4]
    
    # Output CSV data
    print("\n=== ANIMATION FRAMES CSV DATA ===")
    print("Frame 1 (Looking Right):")
    print(frame_to_csv_line(frame1))
    print("\nFrame 2 (Center):")
    print(frame_to_csv_line(frame2))
    print("\nFrame 3 (Looking Left):")
    print(frame_to_csv_line(frame3))
    print("\nFrame 4 (Center):")
    print(frame_to_csv_line(frame4))
    
    # Also create a visual representation for verification
    print("\n=== VISUAL VERIFICATION (Frame 1 - Looking Right) ===")
    for row in frame1:
        line = ""
        for val in row:
            if val == 0:
                line += "."
            else:
                line += "O"
        print(line)

if __name__ == "__main__":
    main()